with census as (
  select 
    population2010 as population, 
    poptype, 
    block.countyid, 
    block.blockid 
  from 
    census_blocks block 
    inner join gtfs_stops stop on st_dwithin(
      block.location, stop.location, 402.335
    ) 
    inner join gtfs_stop_service_map map on map.stopid = stop.id 
    and map.agencyid_def = stop.agencyid 
  where 
    map.agencyid = 'TRIMET' 
    AND block.countyid = '41051' 
  group by 
    block.blockid
), 
employment as (
  select 
    sum(c000_2010) as employment 
  from 
    census 
    left join lodes_rac_projection_block using(blockid) 
  GROUP BY 
    countyid
), 
employees as (
  select 
    sum(c000) as employees 
  from 
    census 
    left join lodes_blocks_wac using(blockid) 
  GROUP BY 
    countyid
), 
urbanpop as (
  select 
    COALESCE(
      sum(population), 
      0
    ) upop 
  from 
    census 
  where 
    poptype = 'U'
), 
ruralpop as (
  select 
    COALESCE(
      sum(population), 
      0
    ) rpop 
  from 
    census 
  where 
    poptype = 'R'
), 
urbanstopcount as (
  select 
    count(stop.id) as urbanstopscount 
  from 
    gtfs_stops stop 
    inner join gtfs_stop_service_map map on map.stopid = stop.id 
    and map.agencyid_def = stop.agencyid 
    inner join census_blocks using(blockid) 
  where 
    map.agencyid = 'TRIMET' 
    AND census_blocks.countyid = '41051' 
    and poptype = 'U'
), 
ruralstopcount as (
  select 
    count(stop.id) as ruralstopscount 
  from 
    gtfs_stops stop 
    inner join gtfs_stop_service_map map on map.stopid = stop.id 
    and map.agencyid_def = stop.agencyid 
    inner join census_blocks using(blockid) 
  where 
    map.agencyid = 'TRIMET' 
    AND census_blocks.countyid = '41051' 
    and poptype = 'R'
), 
routes as (
  select 
    max(
      round(
        (maps.length):: numeric, 
        2
      )
    ) AS length, 
    trip.route_id as routeid 
  from 
    gtfs_trips trip 
    INNER JOIN census_counties_trip_map maps ON trip.id = maps.tripid 
  where 
    trip.agencyid = 'TRIMET' 
    AND maps.countyid = '41051' 
  group by 
    trip.route_id
  order by 
    routeid, 
    length DESC
), 
rmiles as (
  select 
    sum(length) as rtmiles 
  from 
    routes
) 
select 
  COALESCE(urbanstopscount, 0) as urbanstopcount, 
  COALESCE(ruralstopscount, 0) as ruralstopcount, 
  COALESCE(upop, 0) as urbanpop, 
  COALESCE(rpop, 0) as ruralpop, 
  coalesce(employment, 0) as rac, 
  coalesce(employees, 0) as wac, 
  COALESCE(rtmiles, 0) as rtmiles 
from 
  urbanpop 
  inner join ruralpop on true 
  inner join rmiles on true 
  inner join employment on true 
  inner join employees on true 
  inner join urbanstopcount on true 
  inner join ruralstopcount on true;