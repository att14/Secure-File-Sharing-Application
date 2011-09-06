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

	$_SESSION['prof_dept_id'] = $_POST['prof_dept'];
	echo '<form action="view_prof.php" method="post" name="select_prof">';

	$res = $db->prepare("SELECT name, p_id FROM professor WHERE dept_id=" . $_SESSION['prof_dept_id']);
	$res->execute();
	$res->bind_result($name, $p_id);

	$name_arr = array();
	$p_id_arr = array();
	
	while ($res->fetch()) {
		$name_arr[] = $name;
		$p_id_arr[] = $p_id;
	}

	if (count($p_id_arr) == 0) {
		$_SESSION['no_professor'] = 1;
		header("Location: index.php");
	}

	echo 'Professor: ';	
	echo '<br />';	
	createDropdown($name_arr, $p_id_arr, 'prof_name');
	echo '<br />';
	echo '<input type="submit" name="sub_prof" value="Submit">';
	echo '</form>';

	function createDropdown($arr, $val, $frm) {
  		echo '<select name="' . $frm . '" onchange="' . $frm . '.submit();" id="' . $frm . '">';
 		foreach ($arr as $key => $value) {
 			echo '<option value="'.$val[$key].'">'.$value.'</option>';
 		}
	 	echo '</select>';
	}

?>
