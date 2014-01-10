#!/usr/bin/python

import sys
import json, httplib
import os, time
from datetime import datetime

if len(sys.argv) is 2:
	print >> sys.stderr, 'Input file name and build number!'
	exit(1)

fpath = sys.argv[1]
dirs = fpath.split('/')
fname = dirs[len(dirs) - 1]

buildNumber = int(sys.argv[2])

connection = httplib.HTTPSConnection('api.parse.com', 443)
connection.connect()
connection.request('POST', '/1/files/' + fname, open(fpath, 'r'), {
       "X-Parse-Application-Id": "",
       "X-Parse-REST-API-Key": "",
       "Content-Type": "application/binary"
     })

result = json.loads(connection.getresponse().read())
print result

filesize = os.path.getsize(fpath)
filesizestr = "%.2fMB" % (filesize / 1024.0 / 1024.0)

modified = datetime.utcfromtimestamp(os.path.getmtime(fpath)).strftime('%Y-%m-%dT%H:%M:%S.%fZ')
timestamp = datetime.utcfromtimestamp(os.path.getctime(fpath)).strftime('%Y-%m-%dT%H:%M:%S.%fZ')

fchangelog = open('/var/lib/jenkins/jobs/MessageTong-gitlab-master/workspace/changelog.xml', 'r')
changelog = fchangelog.read()

connection.request('POST', '/1/classes/FileList', json.dumps({
  "ACL": {
    "*": {
      "read": True
    },
    "EjpL3wkWDv": {
      "read": True,
      "write": True
    }
  },
  "filename": fname,
  "file": {
    "name": result[
      "name"
    ],
    "__type": "File"
  },
  "size": filesizestr,
  "bytes": filesize,
  "modified": {
    "__type": "Date",
    "iso": modified
  },
  "timestamp": {
    "__type": "Date",
    "iso": timestamp
  },
  "changeLog": changelog,
  "buildNumber": buildNumber,
  "category": "messagetong",
  "package_name": "net.ib.notification"
}),
{
  "X-Parse-Application-Id": "",
  "X-Parse-REST-API-Key": "",
  "Content-Type": "application/json"
})
result = json.loads(connection.getresponse().read())
print result

fchangelog.close()

