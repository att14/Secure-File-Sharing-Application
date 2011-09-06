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

	$_SESSION['course_id'] = $_POST['course_name'];

	$res = $db->prepare("SELECT course_name FROM course WHERE course_id=" . $_SESSION['course_id']);
	$res->execute();
	$res->bind_result($course_name);

	while($res->fetch());

	$res = $db->prepare("SELECT AVG(overall), AVG(class_easiness), AVG(prof_easiness), AVG(prof_helpfulness) FROM rating, class WHERE rating.class_id=class.class_id AND course_id=" . $_SESSION['course_id']);
	$res->execute();
	$res->bind_result($avg_overall, $avg_class_easiness, $avg_prof_easiness, $avg_prof_helpfulness);

	while ($res->fetch());

	$db->query('DROP TABLE IF EXISTS friends');
  	$db->query('CREATE TABLE IF NOT EXISTS friends (fb_id bigint NOT NULL, PRIMARY KEY (fb_id)) ENGINE=MyISAM DEFAULT CHARSET=latin1');

	$db->query('TRUNCATE TABLE friends');
	$access_token = file_get_contents($_SESSION['token']);
	$graph_url = 'https://graph.facebook.com/me/friends?' . $access_token;
	$friends = json_decode(file_get_contents($graph_url));

	foreach ($friends->data as $friend) {
		$db->query('INSERT INTO friends (fb_id) VALUES (' . $friend->id . ')');
	}

	$res = $db->prepare("SELECT AVG(overall), AVG(class_easiness), AVG(prof_easiness), AVG(prof_helpfulness) FROM rating, friends, class WHERE rating.class_id=class.class_id AND friends.fb_id=rating.fb_id AND course_id=" . $_SESSION['course_id']);
	$res->execute();
	$res->bind_result($f_avg_overall, $f_avg_class_easiness, $f_avg_prof_easiness, $f_avg_prof_helpfulness);

	while($res->fetch());

	if ($avg_overall == null)
		$avg_overall = '0.000';	
	if ($avg_class_easiness == null)
		$avg_class_easiness = '0.000';
	if ($avg_prof_easiness == null)
		$avg_prof_easiness = '0.000';
	if ($avg_prof_helpfulness == null)
		$avg_prof_helpfulness = '0.000';

	if ($f_avg_overall == null)
		$f_avg_overall = '0.000';	
	if ($f_avg_class_easiness == null)
		$f_avg_class_easiness = '0.000';
	if ($f_avg_prof_easiness == null)
		$f_avg_prof_easiness = '0.000';
	if ($f_avg_prof_helpfulness == null)
		$f_avg_prof_helpfulness = '0.000';

	echo 'Ratings for ' . $course_name . ':';
	echo '<table border="1" cellspacing="0" cellpadding="7">';
	echo '<tr>';
	echo '<th>Rating</th>'; 
	echo '<th>Average of all Ratings</th>'; 
	echo '<th>Average of Friends Ratings</th>';
	echo '</tr>';
	echo '<tr>'; 
	echo '<td>Overall</td>';
	echo '<td>' . $avg_overall . '</td>';
	echo '<td>' . $f_avg_overall . '</td>';
	echo '</tr>'; 
	echo '<tr>'; 
	echo '<td>Class Easiness</td>';
	echo '<td>' . $avg_class_easiness . '</td>';
	echo '<td>' . $f_avg_class_easiness . '</td>';
	echo '</tr>'; 
	echo '<tr>'; 
	echo '<td>Professor Easiness</td>';
	echo '<td>' . $avg_prof_easiness . '</td>';
	echo '<td>' . $f_avg_prof_easiness . '</td>';
	echo '</tr>'; 
	echo '<tr>'; 
	echo '<td>Professor Helpfulnessl</td>';
	echo '<td>' . $avg_prof_helpfulness . '</td>';
	echo '<td>' . $f_avg_prof_helpfulness . '</td>';
	echo '</tr>'; 
	echo '</table><br />';

	$res = $db->prepare("SELECT fb_fname, fb_lname, season, year, professor.name, review_timestamp, comments FROM appUser, professor, rating, term, class WHERE rating.fb_id=appUser.fb_id AND term.term_id=class.term_id AND rating.class_id=class.class_id AND class.p_id=professor.p_id AND class.course_id=" . $_SESSION['course_id'] . " ORDER BY class.class_id DESC, review_timestamp DESC");
	$res->execute();
	$res->bind_result($fb_fname, $fb_lname, $season, $year, $prof_name, $review_timestamp, $comment);

	while ($res->fetch()) {
		echo $fb_fname . ' ' . $fb_lname . ' commented:<br />';
		echo $comment . '<br />';
		echo $prof_name . ' during ' . $season . ' ' . $year . ' @ ' . $review_timestamp . '<br /><br />';
	}

	echo '<form action="index.php" method="post" name="go_home">';
	echo '<input type="submit" name="home" value="Home">';
	echo '</form>';
?>
