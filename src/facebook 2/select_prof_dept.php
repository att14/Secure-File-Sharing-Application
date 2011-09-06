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

	$_SESSION['prof_univ_id'] = $_POST['prof_univ'];
	echo '<form action="select_prof.php" method="post" name="select_prof_dept">';
	
	$res = $db->prepare("SELECT dept_id FROM professor");
	$res->execute();
	$res->bind_result($dept_id);

	$prof_dept_id_arr = array();

	while ($res->fetch()) {
		$prof_dept_id_arr[] = $dept_id;
	}

	$name_arr = array();
	$dept_id_arr = array();

	foreach($prof_dept_id_arr as $key => $value) {
		$res = $db->prepare("SELECT name, dept_id FROM department WHERE dept_id=" . $value . " AND univ_id=" . $_SESSION['prof_univ_id']);
		$res->execute();
		$res->bind_result($name, $dept_id);
	
		while ($res->fetch()) {
			if (in_array($name, $name_arr)) {
				//do nothing
			} else {
				$name_arr[] = $name;
				$dept_id_arr[] = $dept_id;
			}
		}
	}	

	echo 'Department: ';	
	echo '<br />';	
	createDropdown($name_arr, $dept_id_arr, 'prof_dept');
	echo '<br />';
	echo '<input type="submit" name="sub_prof_dept" value="Submit">';
	echo '</form>';

	function createDropdown($arr, $val, $frm) {
  		echo '<select name="' . $frm . '" onchange="' . $frm . '.submit();" id="' . $frm . '">';
 		foreach ($arr as $key => $value) {
 			echo '<option value="'.$val[$key].'">'.$value.'</option>';
 		}
	 	echo '</select>';
	}

?>
