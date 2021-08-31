extends VehicleBody

# The port we will listen to
const PORT = 9080
# Our WebSocketServer instance
var _server = WebSocketServer.new()

func _ready():
	# Connect base signals to get notified of new client connections,
	# disconnections, and disconnect requests.
	_server.connect("client_connected", self, "_connected")
	_server.connect("client_disconnected", self, "_disconnected")
	_server.connect("client_close_request", self, "_close_request")
	# This signal is emitted when not using the Multiplayer API every time a
	# full packet is received.
	# Alternatively, you could check get_peer(PEER_ID).get_available_packets()
	# in a loop for each connected peer.
	_server.connect("data_received", self, "_on_data")
	# Start listening on the given port.
	var err = _server.listen(PORT)
	if err != OK:
		print("Unable to start server")
		set_process(false)

func _connected(id, proto):
	# This is called when a new peer connects, "id" will be the assigned peer id,
	# "proto" will be the selected WebSocket sub-protocol (which is optional)
	print("Client %d connected with protocol: %s" % [id, proto])
	reset()

func _close_request(id, code, reason):
	# This is called when a client notifies that it wishes to close the connection,
	# providing a reason string and close code.
	print("Client %d disconnecting with code: %d, reason: %s" % [id, code, reason])
	reset()

func _disconnected(id, was_clean = false):
	# This is called when a client disconnects, "id" will be the one of the
	# disconnecting client, "was_clean" will tell you if the disconnection
	# was correctly notified by the remote peer before closing the socket.
	print("Client %d disconnected, clean: %s" % [id, str(was_clean)])
	reset()

func _on_data(id):
	# Print the received packet, you MUST always use get_peer(id).get_packet to receive data,
	# and not get_packet directly when not using the MultiplayerAPI.
	var pkt = _server.get_peer(id).get_packet()
#	print("Got data from client %d: %s" % [id, pkt.get_string_from_utf8()])
	var data = pkt.get_string_from_utf8().split(" ")
	print(data)
	if data[0] == "sensors":
		var sensors = get_sensor_readings()
		_server.get_peer(id).put_var(str(sensors))
	elif data[0] == "wheel":
		set_wheel(int(data[1]), float(data[2]), float(data[3]))
	elif data[0] == "reset":
		reset()

func _process(delta):
	# Call this in _process or _physics_process.
	# Data transfer, and signals emission will only happen when calling this function.
	_server.poll()

func get_sensor_readings(): #returns the sensor values - usual convention of left, right, front, back
	$RangeFinderLeft.force_raycast_update()
	$RangeFinderRight.force_raycast_update()
	$RangeFinderFront.force_raycast_update()
	$RangeFinderBack.force_raycast_update()
	return [$RangeFinderLeft.get_collision_point().distance_to($RangeFinderLeft.global_transform.origin),
		$RangeFinderRight.get_collision_point().distance_to($RangeFinderRight.global_transform.origin),
		$RangeFinderFront.get_collision_point().distance_to($RangeFinderFront.global_transform.origin),
		$RangeFinderBack.get_collision_point().distance_to($RangeFinderBack.global_transform.origin)]

func set_wheel(wheel, engine_force, brake): #sets wheel value - if speed is negative, rotates the wheel (godot only)
	get_node("VehicleWheel" + str(wheel)).engine_force = abs(engine_force)
	get_node("VehicleWheel" + str(wheel)).steering = -PI if engine_force < 0 else 0
	get_node("VehicleWheel" + str(wheel)).brake = brake

func reset():
	global_transform = get_parent().get_node("Position3D").global_transform
	for i in range(4):
		set_wheel(i + 1, 0, 0)
