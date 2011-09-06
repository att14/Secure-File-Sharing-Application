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

	if (isset($_POST['univ_name'])) {
		$university = $_POST['univ_name'];

		if ($university == 'University of Pittsburgh') {
			$university_id = 1;
		} else {
			$university_id = 2;
		}

		$_SESSION['univ_id'] = $university_id;

		$db->query('INSERT INTO appUser (fb_id, fb_username, fb_fname, fb_lname, univ_id) VALUES (' . $_SESSION['id'] . ', "' . $_SESSION['username'] . '", "' . $_SESSION['first_name'] . '", "' . $_SESSION['last_name'] . '", ' . $_SESSION['univ_id'] . ')');
	}

	$res = $db->prepare("SELECT season, year, term_id FROM term");
	$res->execute();
	$res->bind_result($season, $year, $term_id);

	$season_year_arr = array();
	$term_id_arr = array();

	while ($res->fetch()) {
		$season_year_arr[] = $season . ' ' . $year;
		$term_id_arr[] = $term_id;
	}

 	echo '<form action="course.php" method="post" name="term_dept">';
	echo 'Term: ';
	echo '<br />';

	createDropdown($season_year_arr, $term_id_arr, 'term_year');
	echo '<br />';

	$res = $db->prepare("SELECT name, univ_id, dept_id FROM department"); 
 	$res->execute();
 	$res->bind_result($dept_name, $univ_id, $dept_id);
 	
 	$dept_name_arr = array();
	$dept_id_arr = array();
 	
 	while($res->fetch()) {
		if ($univ_id == $_SESSION['univ_id']) {
 			$dept_name_arr[] = $dept_name;
			$dept_id_arr[] = $dept_id;
		}
	}
 	
	echo 'Department: ';
	echo '<br />';
 	createDropdown($dept_name_arr, $dept_id_arr, 'dept_name');
	echo '<br />';
	echo '<br />';
 		
	echo '<INPUT TYPE = "Submit" Name = "sub_dept" VALUE = "Submit">';
 	echo '</form>';

 	function createDropdown($arr, $val, $frm) {
  		echo '<select name="' . $frm . '" onchange="' . $frm . '.submit();" id="' . $frm . '">';
 		
 		foreach ($arr as $key => $value) {
 			echo '<option value="'.$val[$key].'">'.$value.'</option>';
 		}
	 	echo '</select>';
	}
?>
