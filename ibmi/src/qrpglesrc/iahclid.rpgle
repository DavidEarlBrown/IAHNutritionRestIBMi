**FREE
ctl-opt nomain option(*srcstmt:*nodebugio) alwnull(*usrctl);

dcl-proc iahClientList export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);

  exec sql set schema IAHNUTR;
  exec sql
    select coalesce(json_arrayagg(
             json_object(
               'id' value client_id,
               'code' value trim(code),
               'name' value trim(name),
               'contactName' value trim(contact_name),
               'phone' value trim(phone),
               'email' value trim(email),
               'address' value trim(address),
               'city' value trim(city),
               'state' value trim(state),
               'postalCode' value trim(postal_code),
               'country' value trim(country),
               'notes' value trim(notes),
               'active' value case when active = 1 then true else false end
               absent on null
             ) order by name
           ), '[]')
      into :payload
      from client;
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahClientGet export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;
  dcl-s payload varchar(1000000) ccsid(1208);

  exec sql set schema IAHNUTR;
  exec sql
    select json_object(
             'id' value client_id,
             'code' value trim(code),
             'name' value trim(name),
             'contactName' value trim(contact_name),
             'phone' value trim(phone),
             'email' value trim(email),
             'address' value trim(address),
             'city' value trim(city),
             'state' value trim(state),
             'postalCode' value trim(postal_code),
             'country' value trim(country),
             'notes' value trim(notes),
             'active' value case when active = 1 then true else false end
             absent on null
           )
      into :payload
      from client
     where client_id = :id;
  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Client not found"}';
  endif;
  httpStatus = 200;
  return payload;
end-proc;

dcl-proc iahClientCreate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s code varchar(32);
  dcl-s name varchar(128);
  dcl-s contact varchar(128);
  dcl-s phone varchar(32);
  dcl-s email varchar(128);
  dcl-s address varchar(256);
  dcl-s city varchar(64);
  dcl-s state varchar(64);
  dcl-s postal varchar(16);
  dcl-s country varchar(64);
  dcl-s notes varchar(512);
  dcl-s newId packed(15:0);

  exec sql set schema IAHNUTR;
  exec sql
    select upper(x.code), x.name, x.contactName, x.phone, x.email,
           x.address, x.city, x.state, x.postalCode, x.country, x.notes
      into :code, :name, :contact, :phone, :email, :address, :city, :state,
           :postal, :country, :notes
      from json_table(:request, '$'
           columns (
             code varchar(32) path '$.code',
             name varchar(128) path '$.name',
             contactName varchar(128) path '$.contactName',
             phone varchar(32) path '$.phone',
             email varchar(128) path '$.email',
             address varchar(256) path '$.address',
             city varchar(64) path '$.city',
             state varchar(64) path '$.state',
             postalCode varchar(16) path '$.postalCode',
             country varchar(64) path '$.country',
             notes varchar(512) path '$.notes'
           )) as x;

  exec sql
    insert into client (code, name, contact_name, phone, email, address,
                        city, state, postal_code, country, notes)
    values (:code, :name, :contact, :phone, :email, :address,
            :city, :state, :postal, :country, :notes);
  if sqlcode < 0;
    httpStatus = 400;
    return '{"status":400,"message":"Unable to create client"}';
  endif;
  exec sql values identity_val_local() into :newId;
  return iahClientGet(newId: httpStatus);
end-proc;

dcl-proc iahClientUpdate export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    request    varchar(1000000) ccsid(1208) const;
    httpStatus int(10);
  end-pi;
  dcl-s dummy varchar(1000000) ccsid(1208);
  dcl-s code varchar(32);
  dcl-s name varchar(128);
  dcl-s contact varchar(128);
  dcl-s phone varchar(32);
  dcl-s email varchar(128);
  dcl-s address varchar(256);
  dcl-s city varchar(64);
  dcl-s state varchar(64);
  dcl-s postal varchar(16);
  dcl-s country varchar(64);
  dcl-s notes varchar(512);

  dummy = iahClientGet(id: httpStatus);
  if httpStatus = 404;
    return dummy;
  endif;

  exec sql set schema IAHNUTR;
  exec sql
    select upper(x.code), x.name, x.contactName, x.phone, x.email,
           x.address, x.city, x.state, x.postalCode, x.country, x.notes
      into :code, :name, :contact, :phone, :email, :address, :city, :state,
           :postal, :country, :notes
      from json_table(:request, '$'
           columns (
             code varchar(32) path '$.code',
             name varchar(128) path '$.name',
             contactName varchar(128) path '$.contactName',
             phone varchar(32) path '$.phone',
             email varchar(128) path '$.email',
             address varchar(256) path '$.address',
             city varchar(64) path '$.city',
             state varchar(64) path '$.state',
             postalCode varchar(16) path '$.postalCode',
             country varchar(64) path '$.country',
             notes varchar(512) path '$.notes'
           )) as x;

  exec sql
    update client
       set code = :code,
           name = :name,
           contact_name = :contact,
           phone = :phone,
           email = :email,
           address = :address,
           city = :city,
           state = :state,
           postal_code = :postal,
           country = :country,
           notes = :notes,
           updated_at = current_timestamp
     where client_id = :id;
  return iahClientGet(id: httpStatus);
end-proc;

dcl-proc iahClientDelete export;
  dcl-pi *n varchar(1000000) ccsid(1208);
    id         packed(15:0) const;
    httpStatus int(10);
  end-pi;
  exec sql set schema IAHNUTR;
  exec sql delete from client where client_id = :id;
  if sqlcode = 100;
    httpStatus = 404;
    return '{"status":404,"message":"Client not found"}';
  endif;
  httpStatus = 204;
  return '';
end-proc;
