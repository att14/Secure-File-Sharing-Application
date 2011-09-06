<?php
	session_start();

	$dbhost="db7.cs.pitt.edu"; 
	$dbuser="cs1655_att14"; 
	$dbpass="4203"; //last four digits of your peopleSoft id 
	$dbname = "cs1655_att14"; 
	$db = new mysqli($dbhost, $dbuser, $dbpass, $dbname); 
	$app_id = "203470623020222";
	$app_secret = "2cbdee1c819f5f06ee95dd5255b327f5";
	$my_url = "http://cs1520.cs.pitt.edu/~att14/php/";

	$_SESSION['course_dept_id'] = $_POST['course_dept'];
	echo '<form action="view_course.php" method="post" name="select_course">';

	$res = $db->prepare("SELECT course_name, course_id FROM course WHERE dept_id=" . $_SESSION['course_dept_id']);
	$res->execute();
	$res->bind_result($name, $course_id);

	$course_name_arr = array();
	$course_id_arr = array();
	
	while ($res->fetch()) {
		$course_name_arr[] = $name;
		$course_id_arr[] = $course_id;
	}

	if (count($course_id_arr) == 0) {
		$_SESSION['no_course'] = 1;
		header("Location: index.php");
	}

	echo 'Course: ';	
	echo '<br />';	
	createDropdown($course_name_arr, $course_id_arr, 'course_name');
	echo '<br />';
	echo '<input type="submit" name="sub_course" value="Submit">';
	echo '</form>';

	function createDropdown($arr, $val, $frm) {
  		echo '<select name="' . $frm . '" onchange="' . $frm . '.submit();" id="' . $frm . '">';
 		foreach ($arr as $key => $value) {
 			echo '<option value="'.$val[$key].'">'.$value.'</option>';
 		}
	 	echo '</select>';
	}

?>
