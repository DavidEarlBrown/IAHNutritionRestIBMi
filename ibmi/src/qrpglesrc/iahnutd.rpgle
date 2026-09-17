**FREE
ctl-opt nomain option(*srcstmt:*nodebugio) alwnull(*usrctl);

// Nutrient CRUD for IAHNUTR. JSON in/out, SQL only in this module.

dcl-pr iahSyncIngredNut extproc('iahSyncIngredNut');
end-pr;

dcl-proc iahNutrientList export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);

  exec sql set schema IAHNUTR;
  exec sql
    select coalesce(json_arrayagg(
             json_object(
               'id'          value nutrient_id,
               'code'        value trim(code),
               'name'        value trim(name),
               'unit'        value trim(unit),
               'category'    value trim(category),
               'description' value trim(description),
               'active'      value case when active = 1 then true else false end
               absent on null
             )
           ), '[]')
      into :payload
      from nutrient;

  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahNutrientGet export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);

  exec sql set schema IAHNUTR;
  exec sql
    select json_object(
             'id'          value nutrient_id,
             'code'        value trim(code),
             'name'        value trim(name),
             'unit'        value trim(unit),
             'category'    value trim(category),
             'description' value trim(description),
             'active'      value case when active = 1 then true else false end
             absent on null
           )
      into :payload
      from nutrient
     where nutrient_id = :id;

  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Nutrient not found"}';
  endif;
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahNutrientCreate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s code varchar(32);
  dcl-s name varchar(128);
  dcl-s unit varchar(32);
  dcl-s category varchar(32);
  dcl-s description varchar(512);
  dcl-s active ind;
  dcl-s newId packed(15:0);

  exec sql set schema IAHNUTR;
  exec sql
    select upper(x.code), x.name, x.unit, x.category, x.description,
           coalesce(x.active, true)
      into :code, :name, :unit, :category, :description, :active
      from json_table(:request, '$'
           columns (
             code        varchar(32)  path '$.code',
             name        varchar(128) path '$.name',
             unit        varchar(32)  path '$.unit',
             category    varchar(32)  path '$.category',
             description varchar(512) path '$.description',
             active      boolean      path '$.active'
           )) as x;

  exec sql
    insert into nutrient (code, name, unit, category, description, active)
    values (:code, :name, :unit, :category, :description,
            case when :active then 1 else 0 end);

  if sqlcode < 0;
    httpStatus = 400;
    return '{"status":400,"message":"Unable to create nutrient"}';
  endif;
  exec sql values identity_val_local() into :newId;

  exec sql
    insert into ingrednut (ingredient_id, nutrient_id, amount)
    select i.ingredient_id, :newId, 0
      from ingredient i
     where not exists (
             select 1 from ingrednut x
              where x.ingredient_id = i.ingredient_id
                and x.nutrient_id = :newId
           );
  iahSyncIngredNut();

  return iahNutrientGet(newId: httpStatus);
end-proc;

dcl-proc iahNutrientUpdate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s code varchar(32);
  dcl-s name varchar(128);
  dcl-s unit varchar(32);
  dcl-s category varchar(32);
  dcl-s description varchar(512);
  dcl-s active ind;

  exec sql set schema IAHNUTR;
  exec sql
    select upper(x.code), x.name, x.unit, x.category, x.description,
           coalesce(x.active, true)
      into :code, :name, :unit, :category, :description, :active
      from json_table(:request, '$'
           columns (
             code        varchar(32)  path '$.code',
             name        varchar(128) path '$.name',
             unit        varchar(32)  path '$.unit',
             category    varchar(32)  path '$.category',
             description varchar(512) path '$.description',
             active      boolean      path '$.active'
           )) as x;

  exec sql
    update nutrient
       set code = :code,
           name = :name,
           unit = :unit,
           category = :category,
           description = :description,
           active = case when :active then 1 else 0 end,
           updated_at = current_timestamp
     where nutrient_id = :id;

  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Nutrient not found"}';
  endif;
  return iahNutrientGet(id: httpStatus);
end-proc;

dcl-proc iahNutrientDelete export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;

  exec sql set schema IAHNUTR;
  exec sql delete from nutrient where nutrient_id = :id;
  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Nutrient not found"}';
  endif;
  httpStatus = 204;
  return '';
end-proc;
