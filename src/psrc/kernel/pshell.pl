% os independent prolog shell

prolog_root('/Users/tarau/Desktop/sit/openJinni/MainProlog'):-os_name('Mac OS X'),!.
prolog_root('/tarau/prolog').

pshell(Dir,Css):-map(os_call(Dir),Css).

os_type(OType):-val(os,type,OType).
os_type(OType):-
  os_name(OName),
  ( member(OName,['Mac OS X','Linux','Solaris'])->OType=unix
  ; member(OName,['Windows XP','Windows Vista','Windows 7'])->OType=windows
  ; OType=windows
  ).

os_call(Args):-os_call('.',Args).

os_call(Dir,Args):-os_call(Dir,Args,O),println(O).

os_call(Dir,Args,O):-os_type(unix),!,usystem(Dir,Args,O).
os_call(Dir,Args,O):-wsystem(Dir,Args,O).

usystem(Dir,Args,O):-system_call(Dir,Args,O).
wsystem(Dir,Args,O):-system_call(Dir,[cmd,'/c',call|Args],O).

system_call(Dir,Cmd,O):-atomic(Cmd),!,system(Dir,[Cmd],O).
system_call(Dir,Cmd,O):-system(Dir,Cmd,O).

fcopy(From,To):-fcopy('.',From,To).

fcopy(Dir,From,To):-val(os,unix),!,os_call(Dir,[cp,From,To]).
fcopy(Dir,From,To):-os_call(Dir,[copy,From,To]).

fdelete(File):-fdelete('.',File).

fdelete(Dir,File):-val(os,unix),!,os_call(Dir,[rm,'-f',File],_).
fdelete(Dir,File):-os_call(Dir,[del,'/Q',File],_).

mkdir(SubDir):-mkdir('.',SubDir).

mkdir(Dir,SubDir):-val(os,unix),!,os_call(Dir,[mkdir,SubDir]).
mkdir(Dir,SubDir):-os_call(Dir,[mkdir,SubDir]).

rmdir(SubDir):-rmdir('.',SubDir).

rmdir(Dir,SubDir):-val(os,unix),!,os_call(Dir,[rm,'-r','-f',SubDir]).
rmdir(Dir,SubDir):-os_call(Dir,[rmdir,'/Q','/S',SubDir]).

enable_rw(File):-enable_rw('.',File).

enable_rw(Dir,File):-os_type(unix),!,os_call(Dir,[chmod,'a+x+r+w',File]).
enable_rw(Dir,File):-os_call(Dir,['ATTRIB','/D','/S','-R',File]).

prolog(Args):-prolog('.',Args).

ls:-ls('.').
ls(Dir):-dir(Dir).

dir:-dir('.').
dir(Dir):-
  dir2dirs(Dir,Dirs),
  nl,println(dirs:Dirs),nl,
  dir2files(Dir,Files),
  println(files:Files),nl.

% incude 'call(halt)' as last argument to stop the process
prolog(Dir,Args):-
  PJAR= '/bin/prolog.jar',
  make_class_path([PJAR],CP),
  java(Dir,[
    '-Xmx1024M',
    '-Djava.rmi.server.codebase=file:/bin/prolog.jar', 
    '-classpath',CP,
    'prolog.kernel.Main', 
    PJAR|Args]
 ).

make_class_path([],'.'):-!.
make_class_path([P|Ps],NewS):-make_cp_sep(Sep),make_class_path(Ps,S),namecat(P,Sep,S,NewS).

make_cp_sep(Sep):-os_type(unix),!,Sep=':'.
make_cp_sep(Sep):-Sep=';'.

java(Args):-java('.',Args).

java(Dir,Args):-
  println(java=Args),
  os_call(Dir,['java'|Args]).

javac(Dir,Flags,CPs,Subdir,File):-
  namecat(Dir,'/',Subdir,D),
  dir2files(D,Fs),
  findall(F,to_good_file(Subdir,File,Fs,F),GoodFs),
  make_class_path(CPs,CP),
  append(Flags,['-classpath',CP|GoodFs],Args),
  println(javac=Args),
  os_call(Dir,['javac'|Args]).

javac(Dir,Flags,CPs,Subdir):-javac(Dir,Flags,CPs,Subdir,_).

javac(Flags,CPs,Subdir):-javac('.',Flags,CPs,Subdir).

javac(Subdir):-
  javac(['-O','-g','-nowarn'],['/bin/prolog.jar'],Subdir).

to_good_file(Subdir,File,Fs,F):-
  member(File,Fs),
  ends_with(".java",File),
  namecat(Subdir,'/',File,F).

ends_with(Es,Name):-
  length(Es,L),length(Vs,L),
  atom_codes(Name,As),
  append(_,Vs,As),
  !,
  Es=Vs.
  
prolog_bin(PDir):-
  prolog_root(TDir),
  make_cmd([TDir,'/bin'],PDir).
  
testprolog:-
  testprolog(['println(ok)']).
  
testprolog(Args):-
  selfjc,
  selfpc,
  runprolog(Args).

runprolog:-runprolog(['writeq(hello),nl']).

runprolog(Args0):-  
  append(Args0,['halt(0)'],Args),
  prolog_bin(BDir),
  %println(bdir=BDir),
  java(BDir,['prolog.kernel.Main','wam.bp'|Args]).  

selfpc:-
  prolog_bin(PDir),
  prolog(PDir,['and(jboot,halt)']).

selfjc:-map(selfjc,[logic,kernel,core]).
  
selfjc(MDir):-
  prolog_root(TDir),
  make_cmd([prolog,'/',MDir],JC),
  javac(TDir,['-O', '-g', '-nowarn', '-d','bin'],JC). 
