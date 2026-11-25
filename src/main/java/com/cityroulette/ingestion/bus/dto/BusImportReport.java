package com.cityroulette.ingestion.bus.dto;
public record BusImportReport(String provider,boolean dryRun,int fetched,int inserted,int updated,int skipped,int failed,int routeStops) {
    public static BusImportReport empty(String provider,boolean dryRun){return new BusImportReport(provider,dryRun,0,0,0,0,0,0);}
    public BusImportReport add(int fetched,int inserted,int updated,int skipped,int failed,int routeStops){return new BusImportReport(provider,dryRun,this.fetched+fetched,this.inserted+inserted,this.updated+updated,this.skipped+skipped,this.failed+failed,this.routeStops+routeStops);}
}
