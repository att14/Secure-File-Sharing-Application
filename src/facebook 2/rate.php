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

	$course = $_POST['course_name'];
	$prof = $_POST['prof_name'];
	$dept = $_SESSION['dept'];

	$other_course_code = $_POST['other_course_code'];
	$other_course_name = $_POST['other_course_name'];
	$other_prof_name = $_POST['other_prof_name'];
	$other_prof_url = $_POST['other_prof_url'];

	if ($course == 'other') {		
		$db->query('INSERT INTO course (course_code, course_name, dept_id) VALUES ("' . $other_course_code . '", "' . $other_course_name . '", ' . $dept . ')');

		$res = $db->prepare('SELECT course_id FROM course WHERE course_name="' . $other_course_name . '" AND course_code="' . $other_course_code . '"');
		$res->execute();
		$res->bind_result($course);

		while ($res->fetch());
	}	

	if ($prof == 'other') {
		$db->query('INSERT INTO professor (name, url, dept_id) VALUES ("' . $other_prof_name . '", "' . $other_prof_url . '", "' . $dept . '")');

		$res = $db->prepare('SELECT p_id FROM professor WHERE name="' . $other_prof_name . '" AND url="' . $other_prof_url . '"');
		$res->execute();
		$res->bind_result($prof);

		while($res->fetch());	}


	$res = $db->prepare("SELECT class_id FROM class WHERE p_id=".$prof." AND course_id=".$course." AND term_id=".$_SESSION['term']);
	$res->execute();
	$res->bind_result($class_id);

	if($res->fetch()) {
		while($res->fetch());
		$_SESSION['class_id'] = $class_id;
	} else {
		$db->query('INSERT INTO class (course_id, term_id, p_id) VALUES (' . $course . ', ' . $_SESSION['term'] . ', ' . $prof . ')');
		
		$res = $db->prepare("SELECT class_id FROM class WHERE p_id=" . $prof . " AND course_id=" . $course . " AND term_id=" . $_SESSION['term']);
		$res->execute();
		$res->bind_result($class_id);
	
		while($res->fetch());
		$_SESSION['class_id'] = $class_id;		
	}

	$res = $db->prepare("SELECT professor.name, course_name FROM professor, course WHERE professor.p_id=" . $prof . " AND course.course_id=" . $course);
	$res->execute();
	$res->bind_result($prof_name, $course_name);

	while($res->fetch());

	echo ('Rating of ' . $prof_name . ' for ' . $course_name . ':<br /><br />');
	echo '<form action="index.php" method="post" name="term_dept">';
	echo 'Overall Rating: ';
	createRatingDropdown('overall_rating');
	echo '<br />';
	echo 'Class Easiness: ';
	createRatingDropdown('class_easiness');
	echo '<br />';
	echo 'Professor Easiness: ';
	createRatingDropdown('professor_easiness');
	echo '<br />';
	echo 'Professor Helpfulness: ';
	createRatingDropdown('professor_helpfulness');
	echo '<br />';
	echo 'Comments: ';
	echo '<br />';
	echo '<textarea cols="50" rows="20" name="comment" id="comment"></textarea>';
	echo '<br />';
	echo '<input type="submit" name="sub_rating" value="Submit">';
	echo '</form>';


	function createRatingDropdown($frm) {
  		echo '<select name="' . $frm . '" onchange="' . $frm . '.submit();" id="' . $frm . '">';
 		
 		for ($i = 1; $i <= 5; $i++) {
 			echo '<option value="'. $i . '">' . $i . '</option>';
 		}
	 	echo '</select>';
	}

?>
