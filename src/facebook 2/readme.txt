{\rtf1\ansi\ansicpg1252\cocoartf1038\cocoasubrtf320
{\fonttbl\f0\fswiss\fcharset0 Helvetica;}
{\colortbl;\red255\green255\blue255;}
\margl1440\margr1440\vieww9000\viewh8400\viewkind0
\pard\tx720\tx1440\tx2160\tx2880\tx3600\tx4320\tx5040\tx5760\tx6480\tx7200\tx7920\tx8640\ql\qnatural\pardirnatural

\f0\fs24 \cf0 When a user access the app they are asked to allow it to access their Facebook account. Once, that is done the user can choose to Rate Class, View Professor, or View Class. A user is only added to the SQL database if they rate a class. Other than that everything is fairly straight forward.\
\
To access the app go to {\field{\*\fldinst{HYPERLINK "http://cs1520.cs.pitt.edu/~att14/php/"}}{\fldrslt http://cs1520.cs.pitt.edu/~att14/php/}}\
\
You must use bigint for fb_id when initializing the SQL database, otherwise there will be problems with some id numbers. I've included the updated .sql file.}