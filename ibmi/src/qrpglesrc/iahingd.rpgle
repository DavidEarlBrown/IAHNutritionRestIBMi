**FREE
ctl-opt nomain option(*srcstmt:*nodebugio) alwnull(*usrctl);

// Ingredient + composition. INGREDNUT holds one row per ingredient x nutrient.

dcl-pr iahSyncIngredNut extproc('iahSyncIngredNut');
end-pr;

dcl-proc iahIngredientGet export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);

  iahSyncIngredNut();
  exec sql set schema IAHNUTR;
  exec sql
    select json_object(
             'id' value i.ingredient_id,
             'code' value trim(i.code),
             'name' value trim(i.name),
             'category' value trim(i.category),
             'price' value i.price,
             'priceUnit' value trim(i.price_unit),
             'dryMatterPct' value i.dry_matter_pct,
             'density' value i.density,
             'minInclusion' value i.min_inclusion,
             'maxInclusion' value i.max_inclusion,
             'quadraticCost' value i.quadratic_cost,
             'supplier' value trim(i.supplier),
             'notes' value trim(i.notes),
             'active' value case when i.active = 1 then true else false end,
             'nutrients' value (
                select coalesce(json_arrayagg(
                         json_object(
                           'nutrientId' value n.nutrient_id,
                           'nutrientCode' value trim(n.code),
                           'nutrientName' value trim(n.name),
                           'unit' value trim(n.unit),
                           'amount' value c.amount
                         )
                       ), '[]')
                  from ingrednut c
                  join nutrient n on n.nutrient_id = c.nutrient_id
                 where c.ingredient_id = i.ingredient_id
             ),
             'prices' value (
                select coalesce(json_arrayagg(
                         json_object(
                           'id' value p.price_id,
                           'price' value p.price,
                           'priceUnit' value trim(p.price_unit),
                           'effectiveDate' value p.effective_date,
                           'source' value trim(p.source),
                           'notes' value trim(p.notes)
                         )
                       ), '[]')
                  from ingredient_price p
                 where p.ingredient_id = i.ingredient_id
             )
             absent on null
           )
      into :payload
      from ingredient i
     where i.ingredient_id = :id;

  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Ingredient not found"}';
  endif;
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahIngredientList export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    view       varchar(16) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  dcl-s fullView ind;

  fullView = (%upper(%trim(view)) = 'FULL');
  iahSyncIngredNut();
  exec sql set schema IAHNUTR;

  if fullView;
    exec sql
      select coalesce(json_arrayagg(
               json_object(
                 'id' value i.ingredient_id,
                 'code' value trim(i.code),
                 'name' value trim(i.name),
                 'category' value trim(i.category),
                 'price' value i.price,
                 'priceUnit' value trim(i.price_unit),
                 'dryMatterPct' value i.dry_matter_pct,
                 'density' value i.density,
                 'minInclusion' value i.min_inclusion,
                 'maxInclusion' value i.max_inclusion,
                 'quadraticCost' value i.quadratic_cost,
                 'supplier' value trim(i.supplier),
                 'notes' value trim(i.notes),
                 'active' value case when i.active = 1 then true else false end,
                 'nutrients' value (
                    select coalesce(json_arrayagg(
                             json_object(
                               'nutrientId' value n.nutrient_id,
                               'nutrientCode' value trim(n.code),
                               'nutrientName' value trim(n.name),
                               'unit' value trim(n.unit),
                               'amount' value c.amount
                             )
                           ), '[]')
                      from ingrednut c
                      join nutrient n on n.nutrient_id = c.nutrient_id
                     where c.ingredient_id = i.ingredient_id
                 )
                 absent on null
               ) order by i.name
             ), '[]')
        into :payload
        from ingredient i
       where i.active = 1;
  else;
    exec sql
      select coalesce(json_arrayagg(
               json_object(
                 'id' value ingredient_id,
                 'code' value trim(code),
                 'name' value trim(name),
                 'category' value trim(category),
                 'price' value price,
                 'priceUnit' value trim(price_unit),
                 'dryMatterPct' value dry_matter_pct,
                 'minInclusion' value min_inclusion,
                 'maxInclusion' value max_inclusion,
                 'quadraticCost' value quadratic_cost,
                 'supplier' value trim(supplier),
                 'notes' value trim(notes),
                 'active' value case when active = 1 then true else false end
                 absent on null
               ) order by name
             ), '[]')
        into :payload
        from ingredient
       where active = 1;
  endif;

  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahIngredientCreate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s code varchar(32);
  dcl-s name varchar(128);
  dcl-s category varchar(64);
  dcl-s price packed(18:6);
  dcl-s priceUnit varchar(16);
  dcl-s minInc packed(9:6);
  dcl-s maxInc packed(9:6);
  dcl-s qCost packed(18:8);
  dcl-s supplier varchar(128);
  dcl-s notes varchar(512);
  dcl-s newId packed(15:0);

  exec sql set schema IAHNUTR;
  exec sql
    select upper(x.code), x.name, x.category, coalesce(x.price,0),
           coalesce(x.priceUnit,'USD/kg'), coalesce(x.minInclusion,0),
           coalesce(x.maxInclusion,1), coalesce(x.quadraticCost,0),
           x.supplier, x.notes
      into :code, :name, :category, :price, :priceUnit, :minInc, :maxInc,
           :qCost, :supplier, :notes
      from json_table(:request, '$'
           columns (
             code varchar(32) path '$.code',
             name varchar(128) path '$.name',
             category varchar(64) path '$.category',
             price decimal(18,6) path '$.price',
             priceUnit varchar(16) path '$.priceUnit',
             minInclusion decimal(9,6) path '$.minInclusion',
             maxInclusion decimal(9,6) path '$.maxInclusion',
             quadraticCost decimal(18,8) path '$.quadraticCost',
             supplier varchar(128) path '$.supplier',
             notes varchar(512) path '$.notes'
           )) as x;

  exec sql
    insert into ingredient (code, name, category, price, price_unit,
                            min_inclusion, max_inclusion, quadratic_cost,
                            supplier, notes)
    values (:code, :name, :category, :price, :priceUnit,
            :minInc, :maxInc, :qCost, :supplier, :notes);

  if sqlcode < 0;
    httpStatus = 400;
    return '{"status":400,"message":"Unable to create ingredient"}';
  endif;
  exec sql values identity_val_local() into :newId;
  iahSyncIngredNut();

  exec sql
    merge into ingrednut t
    using (
      select x.nutrientId as nutrient_id, x.amount
        from json_table(:request, '$.nutrients[*]'
             columns (
               nutrientId bigint path '$.nutrientId',
               amount decimal(18,8) path '$.amount'
             )) as x
    ) s
       on t.ingredient_id = :newId
      and t.nutrient_id = s.nutrient_id
     when matched then
       update set amount = s.amount, updated_at = current_timestamp;

  return iahIngredientGet(newId: httpStatus);
end-proc;

dcl-proc iahIngredientUpdate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s dummy varchar(1000000) ccsid(1208);

  dummy = iahIngredientGet(id: httpStatus);
  if httpStatus = 404;
    return dummy;
  endif;

  exec sql set schema IAHNUTR;
  exec sql
    update ingredient i
       set (code, name, category, price, price_unit, min_inclusion,
            max_inclusion, quadratic_cost, supplier, notes, updated_at) =
           (select upper(x.code), x.name, x.category, coalesce(x.price,0),
                   coalesce(x.priceUnit,'USD/kg'), coalesce(x.minInclusion,0),
                   coalesce(x.maxInclusion,1), coalesce(x.quadraticCost,0),
                   x.supplier, x.notes, current_timestamp
              from json_table(:request, '$'
                   columns (
                     code varchar(32) path '$.code',
                     name varchar(128) path '$.name',
                     category varchar(64) path '$.category',
                     price decimal(18,6) path '$.price',
                     priceUnit varchar(16) path '$.priceUnit',
                     minInclusion decimal(9,6) path '$.minInclusion',
                     maxInclusion decimal(9,6) path '$.maxInclusion',
                     quadraticCost decimal(18,8) path '$.quadraticCost',
                     supplier varchar(128) path '$.supplier',
                     notes varchar(512) path '$.notes'
                   )) as x)
     where i.ingredient_id = :id;

  exec sql
    merge into ingrednut t
    using (
      select x.nutrientId as nutrient_id, x.amount
        from json_table(:request, '$.nutrients[*]'
             columns (
               nutrientId bigint path '$.nutrientId',
               amount decimal(18,8) path '$.amount'
             )) as x
    ) s
       on t.ingredient_id = :id
      and t.nutrient_id = s.nutrient_id
     when matched then
       update set amount = s.amount, updated_at = current_timestamp;

  iahSyncIngredNut();
  return iahIngredientGet(id: httpStatus);
end-proc;

dcl-proc iahIngredientDelete export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;

  exec sql set schema IAHNUTR;
  exec sql delete from ingredient where ingredient_id = :id;
  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Ingredient not found"}';
  endif;
  httpStatus = 204;
  return '';
end-proc;

dcl-proc iahIngredientUpsertNutrient export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    ingredientId packed(15:0) const;
    request      varchar(1000000) ccsid(1208) const;
    httpStatus   int(10);
  end-pi;
  dcl-s nutrientId packed(15:0);
  dcl-s amount packed(18:8);

  exec sql set schema IAHNUTR;
  exec sql
    select x.nutrientId, x.amount
      into :nutrientId, :amount
      from json_table(:request, '$'
           columns (
             nutrientId bigint path '$.nutrientId',
             amount decimal(18,8) path '$.amount'
           )) as x;

  exec sql
    merge into ingrednut as t
    using (values (:ingredientId, :nutrientId, :amount))
          as s(ingredient_id, nutrient_id, amount)
       on t.ingredient_id = s.ingredient_id
      and t.nutrient_id = s.nutrient_id
     when matched then
       update set amount = s.amount, updated_at = current_timestamp
     when not matched then
       insert (ingredient_id, nutrient_id, amount)
       values (s.ingredient_id, s.nutrient_id, s.amount);

  iahSyncIngredNut();
  return iahIngredientGet(ingredientId: httpStatus);
end-proc;

dcl-proc iahIngredientDeleteNutrient export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    ingredientId packed(15:0) const;
    nutrientId   packed(15:0) const;
    httpStatus   int(10);
  end-pi;

  exec sql set schema IAHNUTR;
  exec sql
    update ingrednut
       set amount = 0,
           updated_at = current_timestamp
     where ingredient_id = :ingredientId
       and nutrient_id = :nutrientId;
  httpStatus = 204;
  return '';
end-proc;

dcl-proc iahIngredientAddPrice export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    ingredientId packed(15:0) const;
    request      varchar(1000000) ccsid(1208) const;
    httpStatus   int(10);
  end-pi;
  dcl-s price packed(18:6);
  dcl-s priceUnit varchar(16);
  dcl-s effDate date;
  dcl-s source varchar(128);
  dcl-s notes varchar(256);
  dcl-s newId packed(15:0);
  dcl-s payload varchar(1000000) ccsid(1208);

  exec sql set schema IAHNUTR;
  exec sql
    select x.price, coalesce(x.priceUnit,'USD/kg'),
           coalesce(x.effectiveDate, current_date), x.source, x.notes
      into :price, :priceUnit, :effDate, :source, :notes
      from json_table(:request, '$'
           columns (
             price decimal(18,6) path '$.price',
             priceUnit varchar(16) path '$.priceUnit',
             effectiveDate date path '$.effectiveDate',
             source varchar(128) path '$.source',
             notes varchar(256) path '$.notes'
           )) as x;

  exec sql
    insert into ingredient_price
      (ingredient_id, price, price_unit, effective_date, source, notes)
    values (:ingredientId, :price, :priceUnit, :effDate, :source, :notes);
  exec sql values identity_val_local() into :newId;
  exec sql
    update ingredient
       set price = :price, price_unit = :priceUnit, updated_at = current_timestamp
     where ingredient_id = :ingredientId;

  exec sql
    select json_object(
             'id' value price_id,
             'price' value price,
             'priceUnit' value trim(price_unit),
             'effectiveDate' value effective_date,
             'source' value trim(source),
             'notes' value trim(notes)
             absent on null
           )
      into :payload
      from ingredient_price
     where price_id = :newId;
  httpStatus = 201;
  return payload;
end-proc;

dcl-proc iahSyncIngredNut export;
  dcl-pi *n;
  end-pi;

  exec sql set schema IAHNUTR;
  exec sql
    insert into ingrednut (ingredient_id, nutrient_id, amount)
    select i.ingredient_id, n.nutrient_id, 0
      from ingredient i
      cross join nutrient n
     where not exists (
             select 1
               from ingrednut x
              where x.ingredient_id = i.ingredient_id
                and x.nutrient_id = n.nutrient_id
           );
end-proc;

dcl-proc iahIngredNutList export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    ingredientId packed(15:0) const;
    httpStatus   int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);

  iahSyncIngredNut();
  exec sql set schema IAHNUTR;
  exec sql
    select coalesce(json_arrayagg(
             json_object(
               'ingredientId' value i.ingredient_id,
               'ingredientCode' value trim(i.code),
               'nutrientId' value n.nutrient_id,
               'nutrientCode' value trim(n.code),
               'nutrientName' value trim(n.name),
               'unit' value trim(n.unit),
               'amount' value c.amount
             )
             order by i.code, n.code
           ), '[]')
      into :payload
      from ingrednut c
      join ingredient i on i.ingredient_id = c.ingredient_id
      join nutrient n on n.nutrient_id = c.nutrient_id
     where :ingredientId = 0
        or c.ingredient_id = :ingredientId;
  httpStatus = 200;
  return payload;
end-proc;
