-- V2 added a provider-agnostic UNIQUE index. V5 later introduced `source` and
-- the correct provider-scoped key, but the old index remained and rejected
-- equal external IDs issued by different providers. H2's MySQL mode accepts
-- the MySQL DROP INDEX form used here.
DROP INDEX uk_places_external_id ON places;

