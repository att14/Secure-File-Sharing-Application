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

	$term = $_POST['term_year'];
	$_SESSION['term'] = $term;
	$dept = $_POST['dept_name'];
	$id = $_SESSION['id'];

	$_SESSION['term'] = $term;
	$_SESSION['dept'] = $dept;
 
	$res = $db->prepare("SELECT course_name, dept_id, course_id FROM course"); 
	$res->execute();
	$res->bind_result($course_name, $dept_id, $course_id);

	$course_name_arr = array();
	$course_id_arr = array();
	
	while ($res->fetch()) {
		if ($dept == $dept_id) {
			$course_name_arr[] = $course_name;
			$course_id_arr[] = $course_id;
		}	
	}

	echo '<form action="rate.php" method="post" name="term_dept">';
	echo 'Course: ';
	echo '<br />';
	createDropdown($course_name_arr, $course_id_arr, 'course_name');
	echo '<br />';
	echo 'If you selected Other... (ex. [CS1550] [Operating Systems]) <br />';
	echo 'Course Code: ';
	echo '<input type="text" name="other_course_code" />';
	echo '<br />';
	echo 'Course Name: ';
	echo '<input type="text" name="other_course_name" />';
	echo '<br />';

	$res = $db->prepare("SELECT p_id, name, dept_id FROM professor");
	$res->execute();
	$res->bind_result($p_id, $name, $dept_id);

	$p_id_arr = array();
	$name_arr = array();

	while ($res->fetch()) {
		if ($dept == $dept_id) {
			$p_id_arr[] = $p_id;
			$name_arr[] = $name;
		}
	}

	echo 'Professor: ';
	echo '<br />';
	createDropdown($name_arr, $p_id_arr, 'prof_name');
	echo '<br />';
	echo 'If you selected Other... (URL not required)<br />';
	echo 'Professor Name: ';
	echo '<input type="text" name="other_prof_name" />';
	echo '<br />';
	echo 'Professor URL: ';
	echo '<input type="text" name="other_prof_url" />';
	echo '<br />';

	echo '<INPUT TYPE = "Submit" Name = "sub_dept" VALUE = "Submit">';
 	echo '</form>';
 		
 	function createDropdown($arr, $val, $frm) {
  		echo '<select name="' . $frm . '" onchange="' . $frm . '.submit();" id="' . $frm . '">';
 		foreach ($arr as $key => $value) {
 			echo '<option value="'.$val[$key].'">'.$value.'</option>';
 		}
		echo '<option value="other">Other</option>';
	 	echo '</select>';
	}
?>
