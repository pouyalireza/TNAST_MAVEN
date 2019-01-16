with svcids as (
  (
    select 
      serviceid_agencyid, 
      serviceid_id, 
      'Tue 01 May 2018' as day 
    from 
      gtfs_calendars gc 
    where 
      startdate :: int <= 20180501 
      and enddate :: int >= 20180501 
      and tuesday = 1 
      and serviceid_agencyid || serviceid_id not in (
        select 
          serviceid_agencyid || serviceid_id 
        from 
          gtfs_calendar_dates 
        where 
          date = '20180501' 
          and exceptiontype = 2
      ) 
    union 
    select 
      serviceid_agencyid, 
      serviceid_id, 
      'Tue 01 May 2018' 
    from 
      gtfs_calendar_dates gcd 
    where 
      date = '20180501' 
      and exceptiontype = 1
  )
), 
regions as (
  select 
    st_union(shape) as rshape, 
    odotregionid 
  from 
    census_counties 
  where 
    odotregionid = 'null' 
  group by 
    odotregionid
), 
trips as (
  select 
    trip.stopscount as stops, 
    trip.tlength as tlength, 
    round(
      (trip.length + trip.estlength):: numeric, 
      2
    ) as length, 
    trip.agencyid as aid, 
    trip.id as tripid, 
    trip.route_id as routeid 
  from 
    svcids 
    inner join gtfs_trips trip using(
      serviceid_agencyid, serviceid_id
    ) 
  where 
    trip.agencyid = 'TRIMET'
), 
service as (
  select 
    COALESCE(
      sum(length), 
      0
    ) as svcmiles, 
    COALESCE(
      sum(tlength), 
      0
    ) as svchours, 
    COALESCE(
      sum(stops), 
      0
    ) as svcstops 
  from 
    trips
), 
stops as (
  select 
    stop.blockid, 
    trips.aid as aid, 
    stime.stop_id as stopid, 
    min(stime.arrivaltime) as arrival, 
    max(stime.departuretime) as departure, 
    stop.location, 
    count(trips.aid) as service 
  from 
    gtfs_stops stop 
    inner join gtfs_stop_times stime on stime.stop_agencyid = stop.agencyid 
    and stime.stop_id = stop.id 
    inner join trips on stime.trip_agencyid = trips.aid 
    and stime.trip_id = trips.tripid 
  group by 
    trips.aid, 
    stime.stop_id, 
    stop.location, 
    stop.blockid
), 
svcstops_urban AS (
  SELECT 
    SUM(service) AS svcstops_urban 
  FROM 
    stops 
    INNER JOIN census_blocks USING(blockid) 
  WHERE 
    poptype = 'U'
), 
svcstops_rural AS (
  SELECT 
    SUM(service) AS svcstops_rural 
  FROM 
    stops 
    INNER JOIN census_blocks USING(blockid) 
  WHERE 
    poptype = 'R'
), 
stops_with_arrivals as (
  select 
    trips.aid as aid, 
    stime.stop_id as stopid, 
    min(stime.arrivaltime) as arrival, 
    max(stime.departuretime) as departure, 
    stop.location, 
    count(trips.aid) as service 
  from 
    gtfs_stops stop 
    inner join gtfs_stop_times stime on stime.stop_agencyid = stop.agencyid 
    and stime.stop_id = stop.id 
    inner join trips on stime.trip_agencyid = trips.aid 
    and stime.trip_id = trips.tripid 
  where 
    stime.arrivaltime > 0 
    and stime.departuretime > 0 
  group by 
    trips.aid, 
    stime.stop_id, 
    stop.location
), 
undupblocks as (
  select 
    urbanid as areaid, 
    block.population2010 as population, 
    block.poptype, 
    block.blockid, 
    sum(stops.service) as service 
  from 
    census_blocks block 
    inner join stops on st_dwithin(
      block.location, stops.location, 402.335
    ) 
  group by 
    block.blockid
), 
svchrs as (
  select 
    COALESCE(
      min(arrival), 
      -1
    ) as fromtime, 
    COALESCE(
      max(departure), 
      -1
    ) as totime 
  from 
    stops_with_arrivals
), 
employment as (
  select 
    sum(c000_2010) as employment, 
    service 
  from 
    undupblocks 
    left join lodes_rac_projection_block using(blockid) 
  group by 
    areaid, 
    service
), 
employees as (
  select 
    sum(c000) as employees, 
    service 
  from 
    undupblocks 
    left join lodes_blocks_wac using(blockid) 
  group by 
    areaid, 
    service
), 
racserved as (
  select 
    COALESCE(
      sum(employment * service), 
      0
    ) as srac 
  from 
    employment
), 
wacserved as (
  select 
    COALESCE(
      sum(employees * service), 
      0
    ) as swac 
  from 
    employees
), 
upopserved as (
  select 
    COALESCE(
      sum(population * service), 
      0
    ) as uspop 
  from 
    undupblocks 
  where 
    poptype = 'U'
), 
rpopserved as (
  select 
    COALESCE(
      sum(population * service), 
      0
    ) as rspop 
  from 
    undupblocks 
  where 
    poptype = 'R'
), 
upop_los as (
  select 
    COALESCE(
      sum(population), 
      0
    ) as upop_los 
  from 
    undupblocks 
  where 
    poptype = 'U' 
    AND service >= 2
), 
rpop_los as (
  select 
    COALESCE(
      sum(population), 
      0
    ) as rpop_los 
  from 
    undupblocks 
  where 
    poptype = 'R' 
    AND service >= 2
), 
svcdays as (
  select 
    COALESCE(
      array_agg(distinct day):: text, 
      '-'
    ) as svdays 
  from 
    svcids
) 
select 
  svcmiles, 
  svchours, 
  svcstops_urban, 
  svcstops_rural, 
  upop_los, 
  rpop_los, 
  uspop, 
  rspop, 
  swac, 
  srac, 
  svdays, 
  fromtime, 
  totime 
from 
  service 
  inner join upopserved on true 
  inner join rpopserved on true 
  inner join svcdays on true 
  inner join racserved on true 
  inner join svchrs on true 
  inner join wacserved on true 
  inner join svcstops_urban on true 
  inner join svcstops_rural on true 
  inner join upop_los on true 
  inner join rpop_los on true