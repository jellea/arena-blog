var https = require('https');

exports.handler = (event, context, callback) => {
  console.log("make request")
  var req = https.request({
      hostname: "api.netlify.com",
      port: 443,
      path: '/build_hooks/5de1cce41738f68985b9c3b1',
      headers: {"Content-Type":"text/plain; charset=utf-8"},
      method: 'POST'
    }, function (res) {
      console.log('statusCode:', res.statusCode);
      console.log('headers:', res.headers);
      var body="";
      res.on("data", (res) =>
        body+=res)
      res.on('end', () =>
        callback(null, {statusCode: res.statusCode, body: res.statusCode == 200 ? "refresh requested, changes should be visible in 30 seconds" : "error!"}))
  })
  req.on("error", (e) =>
     callback(null, {statusCode: 500, body: "oops"})) 
  req.end()
}
