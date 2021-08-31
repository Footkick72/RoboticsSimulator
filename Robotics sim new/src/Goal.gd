extends Spatial


func _on_TopArea_body_entered(body):
	return
	get_node("/root/PointsDisplayLog").report_points(6)


func _on_MidArea_body_entered(body):
	return
	get_node("/root/PointsDisplayLog").report_points(-4)


func _on_BottomArea_body_entered(body):
	return
	get_node("/root/PointsDisplayLog").report_points(2)
