import java.awt.Container;
import java.security.PublicKey;
import javax.swing.JFrame;

/*
 * ConFrame.java
 *
 * Created on Feb 8, 2011, 4:40:16 PM
 */

/**
 *
 * @author CJ
 */
public class ConFrame extends ClientGUI {

   /** Creates new form ConFrame */
    public ConFrame(JFrame parent, UserToken token) {
        super(parent, token);
        parentFrame = parent;
        userToken = token;
        gClient = new GroupClient();

        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        connectButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        userNameField = new javax.swing.JTextField();
        disconnetButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        serverTextField = new javax.swing.JTextField();
        portTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        passwordField = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setName("ConFrame"); // NOI18N

        jLabel1.setText("MPT File Sharing System");

        connectButton.setText("Connect");
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });

        jLabel4.setText("UserName:");

        userNameField.setText("gordon");
        userNameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userNameFieldActionPerformed(evt);
            }
        });

        disconnetButton.setText("Disconnect");
        disconnetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disconnetButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Server:");

        jLabel3.setText("Port:");

        serverTextField.setText("localhost");

        portTextField.setText("8765");

        jLabel5.setText("Password:");

        passwordField.setText("gordon");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addComponent(jLabel1))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addComponent(jLabel4)
                            .addGap(18, 18, 18)
                            .addComponent(userNameField))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel2)
                                .addComponent(jLabel3)
                                .addComponent(jLabel5))
                            .addGap(21, 21, 21)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(serverTextField)
                                .addComponent(portTextField)
                                .addComponent(passwordField, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(disconnetButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(connectButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(userNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(serverTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(portTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(connectButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disconnetButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        String username = userNameField.getText();
        String password = new String(passwordField.getPassword());
        String server = serverTextField.getText();
        int port = Integer.parseInt(portTextField.getText());

        //attempt connection to server and port
        if(gClient.connect(server, port)){

            PublicKey pubKey = gClient.getPubKey();
            System.out.println("Got pubkey");
            byte[][] message = gClient.encrypt(pubKey, username, password);
            System.out.println("userinfo encrypted");
            
            //if server authorizes user
            if(gClient.sendEncryptedUserInfo(message)){
                System.out.println("user info verified");

                //get user token
                userToken = gClient.getToken(userNameField.getText(), "");
                
                if(userToken == null){
                  System.out.println("Illegal Modification, connection dropped.");
                  gClient.disconnect();
                  //System.err.println("Invalid User Token. Check your username and try again.");

                }else{
                    //get encryption keys
                    fileKeys = gClient.getFileKeys(userToken);
                    if(userToken.getGroups().contains("ADMIN")){
                        AdminGroupFrame agFrame = new AdminGroupFrame(parentFrame, userToken, fileKeys, gClient, server, port);
                        Container agPane = agFrame.getContentPane();
                        parentFrame.setContentPane(agPane);
                        parentFrame.pack();
                    }else{
                        UserGroupFrame ugFrame = new UserGroupFrame(parentFrame, userToken, fileKeys, gClient, server, port);
                        Container ugPane = ugFrame.getContentPane();
                        parentFrame.setContentPane(ugPane);
                        parentFrame.pack();
                    }
                }
            }
        }else{
            System.out.println("Failure");
        }
    }//GEN-LAST:event_connectButtonActionPerformed

    private void userNameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userNameFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_userNameFieldActionPerformed

    private void disconnetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disconnetButtonActionPerformed
        this.setVisible(false);
        this.dispose();
        System.exit(0);
    }//GEN-LAST:event_disconnetButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton connectButton;
    private javax.swing.JButton disconnetButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JTextField portTextField;
    private javax.swing.JTextField serverTextField;
    private javax.swing.JTextField userNameField;
    // End of variables declaration//GEN-END:variables

}
