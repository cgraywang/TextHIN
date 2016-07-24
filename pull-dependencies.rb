#!/usr/bin/env ruby

# run this script to copy the dependencies to your local directory.



url = "net.pku.edu.cn/dlib/resources/lib.zip"
localPath = "dlib.zip"

system "wget -c #{url} -O #{localPath}"
system "unzip #{localPath}"
system "rm #{localPath}"