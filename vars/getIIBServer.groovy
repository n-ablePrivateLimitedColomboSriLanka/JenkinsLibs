def call(def branchName) {
    if (branchName == 'production') {
      	["-i ${getActiveACENode('192.168.35.12', '192.168.35.13', '4414')} -p 4414",
         "-i ${getActiveACENode('192.168.35.13', '192.168.35.12', '4415')} -p 4415"j
    } else if (branchName == 'staging') {
        log("Branch Name : ${branchName}")
      	["-i ${getActiveACENode('172.27.31.12', '172.27.31.13', '4414')} -p 4414",
         "-i ${getActiveACENode('172.27.31.13', '172.27.31.12', '4415')} -p 4415"j
    }  else if (branchName == 'development') {
        ["-i 172.27.32.36 -p 4414"]      
    } else {
        ""
    }
}
