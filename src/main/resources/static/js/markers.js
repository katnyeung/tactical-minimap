var stompClient = null;

function connect(callback) {
	var socket = new SockJS('/endpointMarkers'); 
													
	stompClient = Stomp.over(socket);
	stompClient.connect({}, function(frame) {
		console.log('Connected: ' + frame);
		stompClient.subscribe('/markers/push', function(response) {
			console.log(response);
			callback(response);
		});
	});
}

function disconnect() {
	if (stompClient !== null) {
		stompClient.disconnect();
	}
	console.log("Disconnected");
}
