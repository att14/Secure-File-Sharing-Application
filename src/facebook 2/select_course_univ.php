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

	echo '<form action="select_course_dept.php" method="post" name="select_course_univ">';

	$res = $db->prepare("SELECT name, univ_id FROM university");
	$res->execute();
	$res->bind_result($name, $univ_id);

	$name_arr = array();
	$univ_id_arr = array();
	
	while ($res->fetch()) {
		$name_arr[] = $name;
		$univ_id_arr[] = $univ_id;
	}

	echo 'University: ';
	echo '<br />';
	createDropdown($name_arr, $univ_id_arr, 'course_univ');
	echo '<br />';
	echo '<input type="submit" name="sub_course_univ" value="Submit">';

	echo '</form>';

	function createDropdown($arr, $val, $frm) {
  		echo '<select name="' . $frm . '" onchange="' . $frm . '.submit();" id="' . $frm . '">';
 		foreach ($arr as $key => $value) {
 			echo '<option value="'.$val[$key].'">'.$value.'</option>';
 		}
	 	echo '</select>';
	}

?>
