// Scrollable tables: https://datatables.net/examples/index

function Initialize(ws) {
    //var msg = "{ 'surname':'Knight','forename':'Claire', 'flight':1,'count':5,'timestamp':1}"
    //ws.send(msg)
    //log(msg)
}
function log(msg) {
	$("#log").prepend("<p>"+msg+"</p>")
}
$(document).ready(function() {
	var t = $('#table').DataTable();

	// open a WebSocket
	var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
	log("ws"+location.protocol.substring(4)+"//"+window.location.hostname+":9000/bbf/websocket")

    var socket = new WebSocket("ws://localhost:9000/eventsWS")
	socket.onmessage = function(event) {
        var msg = JSON.parse(event.data);
		log("event" + event.data);
		t.row.add( [
            msg.firstName,
            msg.lastName,
            msg.flight,
            msg.count,
            msg.timestamp
        ] ).draw();
    }
    // map initialization
//	var map = Initialize(bbfSocket)

	socket.onopen = function(event) {
	   var msg = '{ "username":"sglover" }'
	   socket.send(msg)
	   log("sent " + msg)
	}

	// if errors on websocket
	var onalert = function(event) {
        $(".alert").removeClass("hide")
        //$("#map").addClass("hide")
        log("websocket connection closed or lost")
    }
	socket.onerror = onalert
	socket.onclose = onalert

/*        
	$("#button").click( function()
    {
		var forename = $("#forename").val();
		var surname = $("#surname").val();
		var flight = $("#flight").val();
		var count = $("#count").val();
		var msg = '{ "lastName":"' + surname + '","firstName":"' + forename + '", "flight":' + flight + ',"count":'
			+ count + ',"timestamp":' + Date.now() + '}'
		log("sending " + msg)
		bbfSocket.send(msg)
    });

	$("#incCount").click( function()
    {
	    $("#count").val(parseInt($("#count").val()) + 1)

		var forename = $("#forename").val();
		var surname = $("#surname").val();
		var flight = $("#flight").val();
		var count = $("#count").val();
		var msg = '{ "lastName":"' + surname + '","firstName":"' + forename + '", "flight":' + flight + ',"count":'
			+ count + ',"timestamp":' + Date.now() + '}'
		log("sending " + msg)
		bbfSocket.send(msg)
	});
	*/
})
