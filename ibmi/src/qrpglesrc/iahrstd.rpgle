**FREE
ctl-opt main(iahRestMain) dftactgrp(*no) actgrp('IAHNUTR')
        option(*srcstmt:*nodebugio) bnddir('QC2LE');

// ILE RPG CGI REST entry point. Apache ScriptAlias /iahdata -> IAHRSTD.
// Java calls these URLs; this program is the only Db2 reader/writer.

dcl-pr iahGetEnv varchar(1024) extproc('iahGetEnv');
  name varchar(64) const;
end-pr;
dcl-pr iahReadBody varchar(1000000) ccsid(1208) extproc('iahReadBody');
end-pr;
dcl-pr iahWriteJson extproc('iahWriteJson');
  status int(10) const;
  body   varchar(1000000) ccsid(1208) const;
end-pr;
dcl-pr iahErrorJson varchar(1024) ccsid(1208) extproc('iahErrorJson');
  status  int(10) const;
  message varchar(512) const;
end-pr;
dcl-pr iahPathSeg varchar(64) extproc('iahPathSeg');
  path varchar(512) const;
  n    int(10) const;
end-pr;

dcl-pr iahNutrientList varchar(1000000) ccsid(1208) extproc('iahNutrientList');
  httpStatus int(10);
end-pr;
dcl-pr iahNutrientGet varchar(1000000) ccsid(1208) extproc('iahNutrientGet');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahNutrientCreate varchar(1000000) ccsid(1208) extproc('iahNutrientCreate');
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahNutrientUpdate varchar(1000000) ccsid(1208) extproc('iahNutrientUpdate');
  id packed(15:0) const;
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahNutrientDelete varchar(1000000) ccsid(1208) extproc('iahNutrientDelete');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;

dcl-pr iahIngredientList varchar(1000000) ccsid(1208) extproc('iahIngredientList');
  view varchar(16) const;
  httpStatus int(10);
end-pr;
dcl-pr iahIngredientGet varchar(1000000) ccsid(1208) extproc('iahIngredientGet');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahIngredientCreate varchar(1000000) ccsid(1208) extproc('iahIngredientCreate');
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahIngredientUpdate varchar(1000000) ccsid(1208) extproc('iahIngredientUpdate');
  id packed(15:0) const;
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahIngredientDelete varchar(1000000) ccsid(1208) extproc('iahIngredientDelete');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahIngredientUpsertNutrient varchar(1000000) ccsid(1208)
       extproc('iahIngredientUpsertNutrient');
  ingredientId packed(15:0) const;
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahIngredientDeleteNutrient varchar(1000000) ccsid(1208)
       extproc('iahIngredientDeleteNutrient');
  ingredientId packed(15:0) const;
  nutrientId packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahIngredientAddPrice varchar(1000000) ccsid(1208)
       extproc('iahIngredientAddPrice');
  ingredientId packed(15:0) const;
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahIngredNutList varchar(1000000) ccsid(1208) extproc('iahIngredNutList');
  ingredientId packed(15:0) const;
  httpStatus int(10);
end-pr;

dcl-pr iahClientList varchar(1000000) ccsid(1208) extproc('iahClientList');
  httpStatus int(10);
end-pr;
dcl-pr iahClientGet varchar(1000000) ccsid(1208) extproc('iahClientGet');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahClientCreate varchar(1000000) ccsid(1208) extproc('iahClientCreate');
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahClientUpdate varchar(1000000) ccsid(1208) extproc('iahClientUpdate');
  id packed(15:0) const;
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahClientDelete varchar(1000000) ccsid(1208) extproc('iahClientDelete');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;

dcl-pr iahSpeciesList varchar(1000000) ccsid(1208) extproc('iahSpeciesList');
  httpStatus int(10);
end-pr;
dcl-pr iahSpeciesGet varchar(1000000) ccsid(1208) extproc('iahSpeciesGet');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahSpeciesCreate varchar(1000000) ccsid(1208) extproc('iahSpeciesCreate');
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahSpeciesUpdate varchar(1000000) ccsid(1208) extproc('iahSpeciesUpdate');
  id packed(15:0) const;
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahStageList varchar(1000000) ccsid(1208) extproc('iahStageList');
  speciesId packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahStageGet varchar(1000000) ccsid(1208) extproc('iahStageGet');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahStageCreate varchar(1000000) ccsid(1208) extproc('iahStageCreate');
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahStageUpdate varchar(1000000) ccsid(1208) extproc('iahStageUpdate');
  id packed(15:0) const;
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;

dcl-pr iahRequirementList varchar(1000000) ccsid(1208) extproc('iahRequirementList');
  speciesId packed(15:0) const;
  stageId packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahRequirementGet varchar(1000000) ccsid(1208) extproc('iahRequirementGet');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahRequirementCreate varchar(1000000) ccsid(1208) extproc('iahRequirementCreate');
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahRequirementUpdate varchar(1000000) ccsid(1208) extproc('iahRequirementUpdate');
  id packed(15:0) const;
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;
dcl-pr iahRequirementDelete varchar(1000000) ccsid(1208) extproc('iahRequirementDelete');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;

dcl-pr iahFormulaList varchar(1000000) ccsid(1208) extproc('iahFormulaList');
  clientId packed(15:0) const;
  speciesId packed(15:0) const;
  stageId packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahFormulaGet varchar(1000000) ccsid(1208) extproc('iahFormulaGet');
  id packed(15:0) const;
  httpStatus int(10);
end-pr;
dcl-pr iahFormulaCreate varchar(1000000) ccsid(1208) extproc('iahFormulaCreate');
  request varchar(1000000) ccsid(1208) const;
  httpStatus int(10);
end-pr;

dcl-proc iahRestMain;
  dcl-s method varchar(12);
  dcl-s path   varchar(512);
  dcl-s query  varchar(512);
  dcl-s body   varchar(1000000) ccsid(1208);
  dcl-s res    varchar(64);
  dcl-s seg2   varchar(64);
  dcl-s seg3   varchar(64);
  dcl-s seg4   varchar(64);
  dcl-s id     packed(15:0);
  dcl-s id2    packed(15:0);
  dcl-s status int(10);
  dcl-s out    varchar(1000000) ccsid(1208);

  method = %upper(iahGetEnv('REQUEST_METHOD'));
  path = iahGetEnv('PATH_INFO');
  query = iahGetEnv('QUERY_STRING');
  body = iahReadBody();
  res = %lower(iahPathSeg(path: 1));
  seg2 = iahPathSeg(path: 2);
  seg3 = %lower(iahPathSeg(path: 3));
  seg4 = iahPathSeg(path: 4);

  select;
    when res = 'nutrients' and seg2 = '' and method = 'GET';
      out = iahNutrientList(status);
    when res = 'nutrients' and seg2 = '' and method = 'POST';
      out = iahNutrientCreate(body: status);
      if status = 200;
        status = 201;
      endif;
    when res = 'nutrients' and method = 'GET';
      id = %dec(seg2:15:0);
      out = iahNutrientGet(id: status);
    when res = 'nutrients' and method = 'PUT';
      id = %dec(seg2:15:0);
      out = iahNutrientUpdate(id: body: status);
    when res = 'nutrients' and method = 'DELETE';
      id = %dec(seg2:15:0);
      out = iahNutrientDelete(id: status);

    when res = 'ingrednut' and method = 'GET';
      out = iahIngredNutList(iahQueryNum(query: 'ingredientId'): status);
    when res = 'ingredients' and seg3 = 'ingrednut' and method = 'GET';
      out = iahIngredNutList(%dec(seg2:15:0): status);
    when res = 'ingredients' and seg2 = '' and method = 'GET';
      out = iahIngredientList(iahQuery(query: 'view'): status);
    when res = 'ingredients' and seg2 = '' and method = 'POST';
      out = iahIngredientCreate(body: status);
      if status = 200;
        status = 201;
      endif;
    when res = 'ingredients' and seg3 = 'nutrients' and seg4 <> '' and method = 'DELETE';
      id = %dec(seg2:15:0);
      id2 = %dec(seg4:15:0);
      out = iahIngredientDeleteNutrient(id: id2: status);
    when res = 'ingredients' and seg3 = 'nutrients' and method = 'PUT';
      id = %dec(seg2:15:0);
      out = iahIngredientUpsertNutrient(id: body: status);
    when res = 'ingredients' and seg3 = 'prices' and method = 'POST';
      id = %dec(seg2:15:0);
      out = iahIngredientAddPrice(id: body: status);
    when res = 'ingredients' and method = 'GET';
      id = %dec(seg2:15:0);
      out = iahIngredientGet(id: status);
    when res = 'ingredients' and method = 'PUT';
      id = %dec(seg2:15:0);
      out = iahIngredientUpdate(id: body: status);
    when res = 'ingredients' and method = 'DELETE';
      id = %dec(seg2:15:0);
      out = iahIngredientDelete(id: status);

    when res = 'clients' and seg2 = '' and method = 'GET';
      out = iahClientList(status);
    when res = 'clients' and seg2 = '' and method = 'POST';
      out = iahClientCreate(body: status);
      if status = 200;
        status = 201;
      endif;
    when res = 'clients' and method = 'GET';
      out = iahClientGet(%dec(seg2:15:0): status);
    when res = 'clients' and method = 'PUT';
      out = iahClientUpdate(%dec(seg2:15:0): body: status);
    when res = 'clients' and method = 'DELETE';
      out = iahClientDelete(%dec(seg2:15:0): status);

    when res = 'species' and seg3 = 'stages' and method = 'GET';
      out = iahStageList(%dec(seg2:15:0): status);
    when res = 'species' and seg2 = '' and method = 'GET';
      out = iahSpeciesList(status);
    when res = 'species' and seg2 = '' and method = 'POST';
      out = iahSpeciesCreate(body: status);
      if status = 200;
        status = 201;
      endif;
    when res = 'species' and method = 'GET';
      out = iahSpeciesGet(%dec(seg2:15:0): status);
    when res = 'species' and method = 'PUT';
      out = iahSpeciesUpdate(%dec(seg2:15:0): body: status);

    when res = 'stages' and seg2 = '' and method = 'POST';
      out = iahStageCreate(body: status);
      if status = 200;
        status = 201;
      endif;
    when res = 'stages' and method = 'GET';
      out = iahStageGet(%dec(seg2:15:0): status);
    when res = 'stages' and method = 'PUT';
      out = iahStageUpdate(%dec(seg2:15:0): body: status);

    when res = 'requirements' and seg2 = '' and method = 'GET';
      out = iahRequirementList(
              iahQueryNum(query: 'speciesId'):
              iahQueryNum(query: 'stageId'):
              status);
    when res = 'requirements' and seg2 = '' and method = 'POST';
      out = iahRequirementCreate(body: status);
      if status = 200;
        status = 201;
      endif;
    when res = 'requirements' and method = 'GET';
      out = iahRequirementGet(%dec(seg2:15:0): status);
    when res = 'requirements' and method = 'PUT';
      out = iahRequirementUpdate(%dec(seg2:15:0): body: status);
    when res = 'requirements' and method = 'DELETE';
      out = iahRequirementDelete(%dec(seg2:15:0): status);

    when res = 'formulas' and seg2 = '' and method = 'GET';
      out = iahFormulaList(
              iahQueryNum(query: 'clientId'):
              iahQueryNum(query: 'speciesId'):
              iahQueryNum(query: 'stageId'):
              status);
    when res = 'formulas' and seg2 = '' and method = 'POST';
      out = iahFormulaCreate(body: status);
      if status = 200;
        status = 201;
      endif;
    when res = 'formulas' and method = 'GET';
      out = iahFormulaGet(%dec(seg2:15:0): status);

    other;
      status = 404;
      out = iahErrorJson(404: 'Unknown data resource');
  endsl;

  iahWriteJson(status: out);
end-proc;

dcl-proc iahQuery;
  dcl-pi *n varchar(64);
    query varchar(512) const;
    name  varchar(32) const;
  end-pi;
  dcl-s token varchar(64);
  dcl-s rest  varchar(512);
  dcl-s amp   int(10);
  dcl-s p     int(10);

  token = name + '=';
  p = %scan(token: query);
  if p = 0;
    return '';
  endif;
  rest = %subst(query: p + %len(token));
  amp = %scan('&': rest);
  if amp > 0;
    return %subst(rest: 1: amp - 1);
  endif;
  return rest;
end-proc;

dcl-proc iahQueryNum;
  dcl-pi *n packed(15:0);
    query varchar(512) const;
    name  varchar(32) const;
  end-pi;
  dcl-s text varchar(64);
  text = iahQuery(query: name);
  if text = *blanks;
    return 0;
  endif;
  return %dec(text:15:0);
end-proc;
