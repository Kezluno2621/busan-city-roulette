package com.cityroulette.tools;

import java.sql.*;
import java.util.*;

public final class ReadOnlyDataVerificationMain {
    private static final List<String> TABLES=List.of("places","place_sources","bus_stops","bus_routes","bus_route_stops");
    private ReadOnlyDataVerificationMain(){ }

    public static void main(String[] args) throws Exception {
        String host=required("DB_HOST"),port=required("DB_PORT"),database=required("DB_NAME");
        String url="jdbc:mysql://"+host+":"+port+"/"+database+"?connectionTimeZone=Asia/Seoul&characterEncoding=UTF-8";
        try(Connection connection=DriverManager.getConnection(url,required("DB_USERNAME"),required("DB_PASSWORD"))){
            connection.setReadOnly(true);connection.setAutoCommit(false);
            System.out.println("DATA_DB connected=true product="+connection.getMetaData().getDatabaseProductName()+" version="+connection.getMetaData().getDatabaseProductVersion());
            Set<String> existing=existingTables(connection);
            for(String table:TABLES)System.out.println("DATA_TABLE "+table+" exists="+existing.contains(table)+(existing.contains(table)?" count="+count(connection,table):""));
            if(existing.contains("places")){
                Set<String> columns=existingColumns(connection,"places");
                if(columns.containsAll(List.of("latitude","longitude")))System.out.println("DATA_PLACE coordinates="+scalar(connection,"SELECT COUNT(*) FROM places WHERE latitude IS NOT NULL AND longitude IS NOT NULL"));
                else System.out.println("DATA_PLACE coordinates=unavailable");
                if(columns.contains("category"))grouped(connection,"SELECT COALESCE(category,'(UNKNOWN)'),COUNT(*) FROM places GROUP BY category ORDER BY category","DATA_CATEGORY");
                else System.out.println("DATA_CATEGORY unavailable=true");
                if(columns.contains("district"))grouped(connection,"SELECT COALESCE(district,'(UNKNOWN)'),COUNT(*) FROM places GROUP BY district ORDER BY district","DATA_DISTRICT");
                else System.out.println("DATA_DISTRICT unavailable=true");
            }
            if(existing.contains("bus_stops"))System.out.println("DATA_BUS_STOP validCoordinates="+scalar(connection,"SELECT COUNT(*) FROM bus_stops WHERE latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180"));
            if(existing.contains("bus_routes")&&existing.contains("bus_route_stops")){
                System.out.println("DATA_ROUTE routesWithoutStops="+scalar(connection,"SELECT COUNT(*) FROM bus_routes r WHERE NOT EXISTS (SELECT 1 FROM bus_route_stops rs WHERE rs.bus_route_id=r.id)"));
                System.out.println("DATA_ROUTE orphanLinks="+scalar(connection,"SELECT COUNT(*) FROM bus_route_stops rs LEFT JOIN bus_routes r ON r.id=rs.bus_route_id LEFT JOIN bus_stops s ON s.id=rs.bus_stop_id WHERE r.id IS NULL OR s.id IS NULL"));
                System.out.println("DATA_ROUTE verifiedOrderedRoutes="+scalar(connection,"SELECT COUNT(*) FROM (SELECT bus_route_id FROM bus_route_stops GROUP BY bus_route_id HAVING MIN(stop_sequence)>=0 AND COUNT(*)=COUNT(DISTINCT stop_sequence)) verified"));
            }
            if(existing.contains("places")&&existing.contains("bus_stops")&&existingColumns(connection,"places").containsAll(List.of("latitude","longitude")))matchingRates(connection);
            else System.out.println("DATA_NEARBY unavailable=true");
            if(existing.contains("flyway_schema_history"))System.out.println("DATA_FLYWAY latestVersion="+textScalar(connection,"SELECT COALESCE(MAX(version),'NONE') FROM flyway_schema_history WHERE success=1"));
            connection.rollback();
        }
    }

    private static Set<String> existingTables(Connection connection)throws SQLException{
        Set<String> result=new HashSet<>();
        try(ResultSet rows=connection.getMetaData().getTables(connection.getCatalog(),null,"%",new String[]{"TABLE"})){
            while(rows.next())result.add(rows.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
        }
        return result;
    }
    private static Set<String> existingColumns(Connection connection,String table)throws SQLException{
        Set<String> result=new HashSet<>();
        try(ResultSet rows=connection.getMetaData().getColumns(connection.getCatalog(),null,table,"%")){
            while(rows.next())result.add(rows.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        return result;
    }
    private static long count(Connection connection,String table)throws SQLException{return scalar(connection,"SELECT COUNT(*) FROM "+table);}
    private static long scalar(Connection connection,String sql)throws SQLException{try(Statement statement=connection.createStatement();ResultSet row=statement.executeQuery(sql)){row.next();return row.getLong(1);}}
    private static String textScalar(Connection connection,String sql)throws SQLException{try(Statement statement=connection.createStatement();ResultSet row=statement.executeQuery(sql)){row.next();return safe(row.getString(1));}}
    private static void grouped(Connection connection,String sql,String prefix)throws SQLException{try(Statement statement=connection.createStatement();ResultSet rows=statement.executeQuery(sql)){while(rows.next())System.out.println(prefix+" value="+safe(rows.getString(1))+" count="+rows.getLong(2));}}
    private static String safe(String value){return value==null?"(UNKNOWN)":value.replace('\n',' ').replace('\r',' ');}
    private static String required(String name){String value=System.getenv(name);if(value==null||value.isBlank())throw new IllegalStateException(name+" is not configured");return value;}

    private static void matchingRates(Connection connection)throws SQLException{
        List<Coordinate> places=coordinates(connection,"SELECT latitude,longitude FROM places WHERE latitude IS NOT NULL AND longitude IS NOT NULL");
        List<Coordinate> stops=coordinates(connection,"SELECT latitude,longitude FROM bus_stops WHERE latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180");
        for(int radius:new int[]{300,500,1000}){
            long matched=places.stream().filter(place->stops.stream().anyMatch(stop->distance(place,stop)<=radius)).count();
            double percent=places.isEmpty()?0.0:Math.round(matched*10_000.0/places.size())/100.0;
            System.out.println("DATA_NEARBY radius="+radius+" locatedPlaces="+places.size()+" matchedPlaces="+matched+" percent="+percent);
        }
    }
    private static List<Coordinate> coordinates(Connection connection,String sql)throws SQLException{
        List<Coordinate> result=new ArrayList<>();try(Statement statement=connection.createStatement();ResultSet rows=statement.executeQuery(sql)){while(rows.next())result.add(new Coordinate(rows.getDouble(1),rows.getDouble(2)));}return result;
    }
    private static double distance(Coordinate a,Coordinate b){double p1=Math.toRadians(a.latitude),p2=Math.toRadians(b.latitude),dLat=p2-p1,dLon=Math.toRadians(b.longitude-a.longitude);double h=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(p1)*Math.cos(p2)*Math.sin(dLon/2)*Math.sin(dLon/2);return 6_371_000*2*Math.atan2(Math.sqrt(h),Math.sqrt(1-h));}
    private record Coordinate(double latitude,double longitude){ }
}
