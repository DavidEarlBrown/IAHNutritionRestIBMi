**FREE
ctl-opt nomain option(*srcstmt:*nodebugio);

// CGI helpers for IAH Nutrition RPG REST (UTF-8 JSON).

dcl-pr QtmhGetEnv extproc('QtmhGetEnv');
  receiver    char(32767) options(*varsize);
  receiverLen int(10)     const;
  responseLen int(10);
  envName     char(256)   const;
  envNameLen  int(10)     const;
  errorCode   char(16)    options(*varsize);
end-pr;

dcl-pr QtmhRdStin extproc('QtmhRdStin');
  receiver    char(32767) options(*varsize);
  receiverLen int(10)     const;
  responseLen int(10);
  errorCode   char(16)    options(*varsize);
end-pr;

dcl-pr QtmhWrStout extproc('QtmhWrStout');
  data        char(32767) options(*varsize);
  dataLen     int(10)     const;
  errorCode   char(16)    options(*varsize);
end-pr;

dcl-proc iahGetEnv export;
  dcl-pi *n varchar(1024);
    name varchar(64) const;
  end-pi;
  dcl-s receiver char(1024);
  dcl-s outLen   int(10);
  dcl-s err      char(16) inz(*allx'00');
  dcl-s envName  char(64);

  envName = name;
  QtmhGetEnv(receiver: %size(receiver): outLen: envName: %len(%trim(name)): err);
  if outLen <= 0;
    return '';
  endif;
  return %subst(receiver: 1: outLen);
end-proc;

dcl-proc iahReadBody export;
  dcl-pi *n varchar(1000000) ccsid(1208);
  end-pi;
  dcl-s chunk    char(32000);
  dcl-s outLen   int(10);
  dcl-s err      char(16) inz(*allx'00');
  dcl-s body     varchar(1000000) ccsid(1208);

  QtmhRdStin(chunk: %size(chunk): outLen: err);
  if outLen > 0;
    body = %subst(chunk: 1: outLen);
  endif;
  return body;
end-proc;

dcl-proc iahWriteJson export;
  dcl-pi *n;
    status int(10) const;
    body   varchar(1000000) ccsid(1208) const;
  end-pi;
  dcl-s header varchar(256);
  dcl-s err    char(16) inz(*allx'00');
  dcl-s text   varchar(1000200);

  header = 'Status: ' + %char(status) + ' '
         + iahStatusText(status) + x'15'
         + 'Content-Type: application/json; charset=utf-8' + x'15'
         + x'15';
  text = header + body;
  QtmhWrStout(%trimr(text): %len(%trimr(text)): err);
end-proc;

dcl-proc iahErrorJson export;
  dcl-pi *n varchar(1024) ccsid(1208);
    status  int(10) const;
    message varchar(512) const;
  end-pi;
  return '{"status":' + %char(status)
       + ',"message":"' + %trim(message) + '"}';
end-proc;

dcl-proc iahStatusText;
  dcl-pi *n varchar(32);
    status int(10) const;
  end-pi;
  select;
    when status = 200;
      return 'OK';
    when status = 201;
      return 'Created';
    when status = 204;
      return 'No Content';
    when status = 400;
      return 'Bad Request';
    when status = 404;
      return 'Not Found';
    other;
      return 'Error';
  endsl;
end-proc;

dcl-proc iahPathSeg export;
  dcl-pi *n varchar(64);
    path varchar(512) const;
    n    int(10) const;
  end-pi;
  dcl-s work varchar(512);
  dcl-s i    int(10) inz(1);
  dcl-s p    int(10);
  dcl-s seg  varchar(64);

  work = %trim(path);
  if %len(work) > 0 and %subst(work:1:1) = '/';
    work = %subst(work:2);
  endif;
  dow i <= n;
    p = %scan('/': work);
    if p = 0;
      if i = n;
        return work;
      endif;
      return '';
    endif;
    seg = %subst(work: 1: p-1);
    if i = n;
      return seg;
    endif;
    work = %subst(work: p+1);
    i += 1;
  enddo;
  return '';
end-proc;
