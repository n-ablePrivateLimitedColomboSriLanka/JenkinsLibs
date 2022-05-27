def call(def branchName) {
    if (branchName == 'production') {
      	["-i 192.168.35.12 -p 4414", "-i 192.168.35.13 -p 4415"] 
    } else if (branchName == 'staging') {
        log("Branch Name : ${branchName}")
      	["-i 172.27.31.12 -p 4414", "-i 172.27.31.13 -p 4415"] 
    }  else if (branchName == 'development') {
        log("Branch Name : ${branchName} 172.27.32.36:4414")
        ["-i 172.27.32.36 -p 4414"]      
    } else {
        ""
    }
}