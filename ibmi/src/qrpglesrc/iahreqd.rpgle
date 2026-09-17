**FREE
ctl-opt nomain option(*srcstmt:*nodebugio) alwnull(*usrctl);

dcl-proc iahRequirementGet export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  exec sql set schema IAHNUTR;
  exec sql
    select json_object(
             'id' value r.requirement_set_id,
             'speciesId' value r.species_id,
             'stageId' value r.stage_id,
             'speciesCode' value trim(p.code),
             'stageCode' value trim(s.code),
             'name' value trim(r.name),
             'sex' value trim(r.sex),
             'breed' value trim(r.breed),
             'productionLevel' value trim(r.production_level),
             'productionValue' value r.production_value,
             'productionUnit' value trim(r.production_unit),
             'bodyWeightKg' value r.body_weight_kg,
             'pregnancyStatus' value trim(r.pregnancy_status),
             'housing' value trim(r.housing),
             'environment' value trim(r.environment),
             'activityLevel' value trim(r.activity_level),
             'source' value trim(r.source),
             'notes' value trim(r.notes),
             'active' value case when r.active = 1 then true else false end,
             'lines' value (
                select coalesce(json_arrayagg(
                         json_object(
                           'id' value l.requirement_line_id,
                           'nutrientId' value n.nutrient_id,
                           'nutrientCode' value trim(n.code),
                           'nutrientName' value trim(n.name),
                           'unit' value trim(n.unit),
                           'minValue' value l.min_value,
                           'maxValue' value l.max_value,
                           'targetValue' value l.target_value
                           absent on null
                         )
                       ), '[]')
                  from requirement_line l
                  join nutrient n on n.nutrient_id = l.nutrient_id
                 where l.requirement_set_id = r.requirement_set_id
             )
             absent on null
           )
      into :payload
      from requirement_set r
      join animal_species p on p.species_id = r.species_id
      join animal_stage s on s.stage_id = r.stage_id
     where r.requirement_set_id = :id;
  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Requirement set not found"}';
  endif;
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahRequirementList export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    speciesId  packed(15:0) const;
    stageId    packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  exec sql set schema IAHNUTR;
  exec sql
    select coalesce(json_arrayagg(doc), '[]')
      into :payload
      from requirement_set r
      join animal_species p on p.species_id = r.species_id
      join animal_stage s on s.stage_id = r.stage_id
      cross join table(values json_object(
             'id' value r.requirement_set_id,
             'speciesId' value r.species_id,
             'stageId' value r.stage_id,
             'speciesCode' value trim(p.code),
             'stageCode' value trim(s.code),
             'name' value trim(r.name),
             'sex' value trim(r.sex),
             'breed' value trim(r.breed),
             'productionLevel' value trim(r.production_level),
             'housing' value trim(r.housing),
             'environment' value trim(r.environment),
             'active' value case when r.active = 1 then true else false end
           )) as x(doc)
     where r.active = 1
       and (:speciesId = 0 or r.species_id = :speciesId)
       and (:stageId = 0 or r.stage_id = :stageId);
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahRequirementCreate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s speciesId packed(15:0);
  dcl-s stageId packed(15:0);
  dcl-s name varchar(128);
  dcl-s sex varchar(16);
  dcl-s breed varchar(64);
  dcl-s production varchar(32);
  dcl-s housing varchar(64);
  dcl-s environment varchar(64);
  dcl-s newId packed(15:0);

  exec sql set schema IAHNUTR;
  exec sql
    select x.speciesId, x.stageId, x.name, upper(x.sex), x.breed,
           upper(x.productionLevel), upper(x.housing), upper(x.environment)
      into :speciesId, :stageId, :name, :sex, :breed, :production, :housing,
           :environment
      from json_table(:request, '$'
           columns (
             speciesId bigint path '$.speciesId',
             stageId bigint path '$.stageId',
             name varchar(128) path '$.name',
             sex varchar(16) path '$.sex',
             breed varchar(64) path '$.breed',
             productionLevel varchar(32) path '$.productionLevel',
             housing varchar(64) path '$.housing',
             environment varchar(64) path '$.environment'
           )) as x;

  exec sql
    insert into requirement_set
      (species_id, stage_id, name, sex, breed, production_level, housing, environment)
    values (:speciesId, :stageId, :name, :sex, :breed, :production, :housing, :environment);
  exec sql values identity_val_local() into :newId;

  exec sql
    insert into requirement_line
      (requirement_set_id, nutrient_id, min_value, max_value, target_value)
    select :newId, x.nutrientId, x.minValue, x.maxValue, x.targetValue
      from json_table(:request, '$.lines[*]'
           columns (
             nutrientId bigint path '$.nutrientId',
             minValue decimal(18,8) path '$.minValue',
             maxValue decimal(18,8) path '$.maxValue',
             targetValue decimal(18,8) path '$.targetValue'
           )) as x;

  return iahRequirementGet(newId: httpStatus);
end-proc;

dcl-proc iahRequirementDelete export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;
  exec sql set schema IAHNUTR;
  exec sql delete from requirement_set where requirement_set_id = :id;
  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Requirement set not found"}';
  endif;
  httpStatus = 204;
  return '';
end-proc;

dcl-proc iahRequirementUpdate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s dummy varchar(1000000) ccsid(1208);
  dummy = iahRequirementGet(id: httpStatus);
  if httpStatus = 404;
    return dummy;
  endif;
  exec sql set schema IAHNUTR;
  exec sql delete from requirement_line where requirement_set_id = :id;
  exec sql delete from requirement_set where requirement_set_id = :id;
  return iahRequirementCreate(request: httpStatus);
end-proc;
