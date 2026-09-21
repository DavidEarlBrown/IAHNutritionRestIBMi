**FREE
ctl-opt nomain option(*srcstmt:*nodebugio) alwnull(*usrctl);

// Save/load a complete formula (header + ingredients + nutrients) in one commit.

dcl-proc iahFormulaGet export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  exec sql set schema IAHNUTR;
  exec sql
    select json_object(
             'id' value f.formula_id,
             'code' value trim(f.code),
             'name' value trim(f.name),
             'clientId' value f.client_id,
             'clientName' value trim(c.name),
             'status' value trim(f.status),
             'speciesId' value f.species_id,
             'speciesCode' value trim(p.code),
             'stageId' value f.stage_id,
             'stageCode' value trim(s.code),
             'animalAgeDays' value f.animal_age_days,
             'sex' value trim(f.sex),
             'breed' value trim(f.breed),
             'productionLevel' value trim(f.production_level),
             'housing' value trim(f.housing),
             'environment' value trim(f.environment),
             'batchWeightKg' value f.batch_weight_kg,
             'totalCost' value f.total_cost,
             'costPerKg' value f.cost_per_kg,
             'optimizationType' value trim(f.optimization_type),
             'solverStatus' value trim(f.solver_status),
             'objectiveValue' value f.objective_value,
             'notes' value trim(f.notes),
             'createdAt' value f.created_at,
             'ingredients' value (
                select coalesce(json_arrayagg(
                         json_object(
                           'ingredientId' value g.ingredient_id,
                           'ingredientCode' value trim(i.code),
                           'ingredientName' value trim(i.name),
                           'inclusionFrac' value g.inclusion_frac,
                           'inclusionPct' value g.inclusion_frac * 100,
                           'amountKg' value g.amount_kg,
                           'priceUsed' value g.price_used,
                           'lastPrice' value g.last_price,
                           'cost' value g.cost
                         )
                       ), '[]')
                  from formula_ingredient g
                  join ingredient i on i.ingredient_id = g.ingredient_id
                 where g.formula_id = f.formula_id
             ),
             'nutrients' value (
                select coalesce(json_arrayagg(
                         json_object(
                           'nutrientId' value n.nutrient_id,
                           'nutrientCode' value trim(n.code),
                           'nutrientName' value trim(n.name),
                           'unit' value trim(n.unit),
                           'achievedValue' value t.achieved_value,
                           'minValue' value t.min_value,
                           'maxValue' value t.max_value,
                           'targetValue' value t.target_value
                           absent on null
                         )
                       ), '[]')
                  from formula_nutrient t
                  join nutrient n on n.nutrient_id = t.nutrient_id
                 where t.formula_id = f.formula_id
             )
             absent on null
           )
      into :payload
      from formula f
      left join client c on c.client_id = f.client_id
      join animal_species p on p.species_id = f.species_id
      join animal_stage s on s.stage_id = f.stage_id
     where f.formula_id = :id;
  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Formula not found"}';
  endif;
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahFormulaList export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    clientId   packed(15:0) const;
    speciesId  packed(15:0) const;
    stageId    packed(15:0) const;
    formulaStatus varchar(16) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  dcl-s wanted varchar(16);
  wanted = %upper(%trim(formulaStatus));
  exec sql set schema IAHNUTR;
  exec sql
    select coalesce(json_arrayagg(
             json_object(
               'id' value f.formula_id,
               'code' value trim(f.code),
               'name' value trim(f.name),
               'clientId' value f.client_id,
               'clientName' value trim(c.name),
               'status' value trim(f.status),
               'speciesId' value f.species_id,
               'speciesCode' value trim(p.code),
               'stageId' value f.stage_id,
               'stageCode' value trim(s.code),
               'animalAgeDays' value f.animal_age_days,
               'batchWeightKg' value f.batch_weight_kg,
               'totalCost' value f.total_cost,
               'costPerKg' value f.cost_per_kg,
               'optimizationType' value trim(f.optimization_type),
               'solverStatus' value trim(f.solver_status),
               'createdAt' value f.created_at
               absent on null
             ) order by f.created_at desc
           ), '[]')
      into :payload
      from formula f
      left join client c on c.client_id = f.client_id
      join animal_species p on p.species_id = f.species_id
      join animal_stage s on s.stage_id = f.stage_id
     where (:clientId = 0
            or f.client_id = :clientId
            or (c.is_default = 1
                and exists (select 1 from client q
                             where q.client_id = :clientId
                               and q.is_default = 0)))
       and (:speciesId = 0 or f.species_id = :speciesId)
       and (:stageId = 0 or f.stage_id = :stageId)
       and (:wanted = '' or f.status = :wanted);
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahFormulaCreate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s newId packed(15:0);

  exec sql set schema IAHNUTR;
  exec sql
    insert into formula
      (code, name, client_id, status, species_id, stage_id, animal_age_days, sex, breed,
       production_level, housing, environment, batch_weight_kg, total_cost,
       cost_per_kg, optimization_type, solver_status, objective_value, notes)
    select x.code, x.name,
           coalesce(x.clientId,
                    (select client_id from client where is_default = 1
                       fetch first 1 row only)),
           coalesce(upper(x.status), 'PREFIXED'),
           x.speciesId, x.stageId, x.animalAgeDays,
           x.sex, x.breed, x.productionLevel, x.housing, x.environment,
           coalesce(x.batchWeightKg, 1000), x.totalCost, x.costPerKg,
           x.optimizationType, x.solverStatus,
           x.objectiveValue, x.notes
      from json_table(:request, '$'
           columns (
             code varchar(32) path '$.code',
             name varchar(128) path '$.name',
             clientId bigint path '$.clientId',
             status varchar(16) path '$.status',
             speciesId bigint path '$.speciesId',
             stageId bigint path '$.stageId',
             animalAgeDays integer path '$.animalAgeDays',
             sex varchar(16) path '$.sex',
             breed varchar(64) path '$.breed',
             productionLevel varchar(32) path '$.productionLevel',
             housing varchar(64) path '$.housing',
             environment varchar(64) path '$.environment',
             batchWeightKg decimal(18,6) path '$.batchWeightKg',
             totalCost decimal(18,6) path '$.totalCost',
             costPerKg decimal(18,6) path '$.costPerKg',
             optimizationType varchar(16) path '$.optimizationType',
             solverStatus varchar(32) path '$.solverStatus',
             objectiveValue decimal(18,8) path '$.objectiveValue',
             notes varchar(512) path '$.notes'
           )) as x;

  if sqlcode < 0;
    exec sql rollback;
    httpStatus = 400;
    return '{"status":400,"message":"Unable to save formula"}';
  endif;
  exec sql values identity_val_local() into :newId;

  exec sql
    insert into formula_ingredient
      (formula_id, ingredient_id, inclusion_frac, amount_kg, price_used, last_price, cost)
    select :newId, x.ingredientId, x.inclusionFrac, x.amountKg,
           coalesce(x.priceUsed, x.lastPrice, 0),
           coalesce(x.lastPrice, x.priceUsed, 0),
           x.cost
      from json_table(:request, '$.ingredients[*]'
           columns (
             ingredientId bigint path '$.ingredientId',
             inclusionFrac decimal(18,10) path '$.inclusionFrac',
             amountKg decimal(18,6) path '$.amountKg',
             priceUsed decimal(18,6) path '$.priceUsed',
             lastPrice decimal(18,6) path '$.lastPrice',
             cost decimal(18,6) path '$.cost'
           )) as x;

  exec sql
    insert into formula_nutrient
      (formula_id, nutrient_id, achieved_value, min_value, max_value, target_value)
    select :newId, x.nutrientId, x.achievedValue, x.minValue, x.maxValue, x.targetValue
      from json_table(:request, '$.nutrients[*]'
           columns (
             nutrientId bigint path '$.nutrientId',
             achievedValue decimal(18,8) path '$.achievedValue',
             minValue decimal(18,8) path '$.minValue',
             maxValue decimal(18,8) path '$.maxValue',
             targetValue decimal(18,8) path '$.targetValue'
           )) as x;

  exec sql
    insert into client_formulas_hdr
      (formula_id, client_id, formula_description, last_price, animal_id,
       optimization_technique)
    select formula_id, client_id, name, coalesce(cost_per_kg, total_cost),
           species_id, optimization_type
      from formula
     where formula_id = :newId;

  exec sql commit;
  return iahFormulaGet(newId: httpStatus);
end-proc;

dcl-proc iahClientFormulasHdrList export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    clientId   packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  exec sql set schema IAHNUTR;
  exec sql
    select coalesce(json_arrayagg(
             json_object(
               'clientId' value h.client_id,
               'formulaId' value h.formula_id,
               'formulaDescription' value trim(h.formula_description),
               'lastPrice' value h.last_price,
               'animalId' value h.animal_id,
               'optimizationTechnique' value trim(h.optimization_technique)
               absent on null
             ) order by h.formula_id
           ), '[]')
      into :payload
      from client_formulas_hdr h
      left join client c on c.client_id = h.client_id
     where (:clientId = 0
            or h.client_id = :clientId
            or (c.is_default = 1
                and exists (select 1 from client q
                             where q.client_id = :clientId
                               and q.is_default = 0)));
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahClientFormulasHdrGet export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);
  exec sql set schema IAHNUTR;
  exec sql
    select json_object(
             'clientId' value h.client_id,
             'formulaId' value h.formula_id,
             'formulaDescription' value trim(h.formula_description),
             'lastPrice' value h.last_price,
             'animalId' value h.animal_id,
             'optimizationTechnique' value trim(h.optimization_technique)
             absent on null
           )
      into :payload
      from client_formulas_hdr h
     where h.formula_id = :id;
  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Client formula header not found"}';
  endif;
  httpStatus = 200;
  return payload;
end-proc;
