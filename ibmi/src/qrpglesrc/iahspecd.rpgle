**FREE
ctl-opt nomain option(*srcstmt:*nodebugio) alwnull(*usrctl);

dcl-proc iahSpeciesList export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  exec sql set schema IAHNUTR;
  exec sql
    select coalesce(json_arrayagg(
             json_object(
               'id' value species_id,
               'code' value trim(code),
               'name' value trim(name),
               'description' value trim(description),
               'active' value case when active = 1 then true else false end
               absent on null
             ) order by name
           ), '[]')
      into :payload
      from animal_species;
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahSpeciesGet export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  exec sql set schema IAHNUTR;
  exec sql
    select json_object(
             'id' value species_id,
             'code' value trim(code),
             'name' value trim(name),
             'description' value trim(description),
             'active' value case when active = 1 then true else false end
             absent on null
           )
      into :payload
      from animal_species
     where species_id = :id;
  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Species not found"}';
  endif;
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahSpeciesCreate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s code varchar(32);
  dcl-s name varchar(128);
  dcl-s description varchar(512);
  dcl-s newId packed(15:0);
  exec sql set schema IAHNUTR;
  exec sql
    select upper(x.code), x.name, x.description
      into :code, :name, :description
      from json_table(:request, '$'
           columns (
             code varchar(32) path '$.code',
             name varchar(128) path '$.name',
             description varchar(512) path '$.description'
           )) as x;
  exec sql
    insert into animal_species (code, name, description)
    values (:code, :name, :description);
  exec sql values identity_val_local() into :newId;
  return iahSpeciesGet(newId: httpStatus);
end-proc;

dcl-proc iahSpeciesUpdate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s dummy varchar(1000000) ccsid(1208);
  dcl-s code varchar(32);
  dcl-s name varchar(128);
  dcl-s description varchar(512);
  dummy = iahSpeciesGet(id: httpStatus);
  if httpStatus = 404;
    return dummy;
  endif;
  exec sql set schema IAHNUTR;
  exec sql
    select upper(x.code), x.name, x.description
      into :code, :name, :description
      from json_table(:request, '$'
           columns (
             code varchar(32) path '$.code',
             name varchar(128) path '$.name',
             description varchar(512) path '$.description'
           )) as x;
  exec sql
    update animal_species
       set code = :code, name = :name, description = :description
     where species_id = :id;
  return iahSpeciesGet(id: httpStatus);
end-proc;

dcl-proc iahStageList export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    speciesId  packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  exec sql set schema IAHNUTR;
  exec sql
    select coalesce(json_arrayagg(
             json_object(
               'id' value s.stage_id,
               'speciesId' value s.species_id,
               'speciesCode' value trim(p.code),
               'code' value trim(s.code),
               'name' value trim(s.name),
               'ageMinDays' value s.age_min_days,
               'ageMaxDays' value s.age_max_days,
               'weightMinKg' value s.weight_min_kg,
               'weightMaxKg' value s.weight_max_kg,
               'description' value trim(s.description),
               'active' value case when s.active = 1 then true else false end
               absent on null
             ) order by s.age_min_days
           ), '[]')
      into :payload
      from animal_stage s
      join animal_species p on p.species_id = s.species_id
     where s.species_id = :speciesId
       and s.active = 1;
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahStageGet export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  exec sql set schema IAHNUTR;
  exec sql
    select json_object(
             'id' value s.stage_id,
             'speciesId' value s.species_id,
             'speciesCode' value trim(p.code),
             'code' value trim(s.code),
             'name' value trim(s.name),
             'ageMinDays' value s.age_min_days,
             'ageMaxDays' value s.age_max_days,
             'weightMinKg' value s.weight_min_kg,
             'weightMaxKg' value s.weight_max_kg,
             'description' value trim(s.description),
             'active' value case when s.active = 1 then true else false end
             absent on null
           )
      into :payload
      from animal_stage s
      join animal_species p on p.species_id = s.species_id
     where s.stage_id = :id;
  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Animal stage/age group not found"}';
  endif;
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahStageCreate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s speciesId packed(15:0);
  dcl-s code varchar(32);
  dcl-s name varchar(128);
  dcl-s ageMin int(10);
  dcl-s ageMax int(10);
  dcl-s wMin packed(9:3);
  dcl-s wMax packed(9:3);
  dcl-s description varchar(512);
  dcl-s newId packed(15:0);
  exec sql set schema IAHNUTR;
  exec sql
    select x.speciesId, upper(x.code), x.name, x.ageMinDays, x.ageMaxDays,
           x.weightMinKg, x.weightMaxKg, x.description
      into :speciesId, :code, :name, :ageMin, :ageMax, :wMin, :wMax, :description
      from json_table(:request, '$'
           columns (
             speciesId bigint path '$.speciesId',
             code varchar(32) path '$.code',
             name varchar(128) path '$.name',
             ageMinDays integer path '$.ageMinDays',
             ageMaxDays integer path '$.ageMaxDays',
             weightMinKg decimal(9,3) path '$.weightMinKg',
             weightMaxKg decimal(9,3) path '$.weightMaxKg',
             description varchar(512) path '$.description'
           )) as x;
  exec sql
    insert into animal_stage
      (species_id, code, name, age_min_days, age_max_days,
       weight_min_kg, weight_max_kg, description)
    values (:speciesId, :code, :name, :ageMin, :ageMax, :wMin, :wMax, :description);
  exec sql values identity_val_local() into :newId;
  return iahStageGet(newId: httpStatus);
end-proc;

dcl-proc iahStageUpdate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s dummy varchar(1000000) ccsid(1208);
  dcl-s speciesId packed(15:0);
  dcl-s code varchar(32);
  dcl-s name varchar(128);
  dcl-s ageMin int(10);
  dcl-s ageMax int(10);
  dcl-s wMin packed(9:3);
  dcl-s wMax packed(9:3);
  dcl-s description varchar(512);

  dummy = iahStageGet(id: httpStatus);
  if httpStatus = 404;
    return dummy;
  endif;
  exec sql set schema IAHNUTR;
  exec sql
    select x.speciesId, upper(x.code), x.name, x.ageMinDays, x.ageMaxDays,
           x.weightMinKg, x.weightMaxKg, x.description
      into :speciesId, :code, :name, :ageMin, :ageMax, :wMin, :wMax, :description
      from json_table(:request, '$'
           columns (
             speciesId bigint path '$.speciesId',
             code varchar(32) path '$.code',
             name varchar(128) path '$.name',
             ageMinDays integer path '$.ageMinDays',
             ageMaxDays integer path '$.ageMaxDays',
             weightMinKg decimal(9,3) path '$.weightMinKg',
             weightMaxKg decimal(9,3) path '$.weightMaxKg',
             description varchar(512) path '$.description'
           )) as x;
  exec sql
    update animal_stage
       set species_id = :speciesId,
           code = :code,
           name = :name,
           age_min_days = :ageMin,
           age_max_days = :ageMax,
           weight_min_kg = :wMin,
           weight_max_kg = :wMax,
           description = :description
     where stage_id = :id;
  return iahStageGet(id: httpStatus);
end-proc;
