import java.io.File;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
/*
 * DownDelFrame.java
 *
 * Created on Feb 8, 2011, 7:41:33 PM
 *
 * @author CJ
 */
public class DownDelFrame extends ClientGUI {
    private FileClient fClient;
    private String server;
    private int port;

    /** Creates new form DownDelFrame */
    public DownDelFrame(JFrame parent, UserToken token, HashMap<String, ArrayList<Key>> keys, FileClient fileClient,
            String serverLoc, int portNum) {
        super(parent, token);
        fClient = fileClient;
        server = serverLoc;
        port = portNum;
        fileKeys = keys;
        initComponents();

        ArrayList<String> fileList = (ArrayList<String>) fClient.listFiles(userToken);
        fileListPane.setListData(fileList.toArray());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        jScrollPane1 = new javax.swing.JScrollPane();
        fileListPane = new javax.swing.JList();
        jLabel1 = new javax.swing.JLabel();
        backButton = new javax.swing.JButton();
        downButton = new javax.swing.JButton();
        delButton = new javax.swing.JButton();
        refreshButton = new javax.swing.JButton();

        jList1.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(jList1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jScrollPane1.setViewportView(fileListPane);

        jLabel1.setText("Files you can download and/or delete:");

        backButton.setText("Back");
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        downButton.setText("Download Selected File(s)");
        downButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downButtonActionPerformed(evt);
            }
        });

        delButton.setText("Delete Selected File(s)");
        delButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delButtonActionPerformed(evt);
            }
        });

        refreshButton.setText("Refresh Files");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(backButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(delButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(downButton))
                    .addComponent(refreshButton))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(refreshButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(backButton)
                    .addComponent(downButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(delButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void downButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downButtonActionPerformed
        Object[] filesToDown = fileListPane.getSelectedValues();
        boolean downSuccess;

        for(int i=0; i<filesToDown.length; i++){
            downSuccess = fClient.download((String) filesToDown[i], (String) filesToDown[i], userToken, fileKeys);
            if(downSuccess){
                System.out.println("File " + filesToDown[i] + "successfully downloaded");
            }else{
                System.err.println("File " + filesToDown[i] + "download failed");
            }
        }
    }//GEN-LAST:event_downButtonActionPerformed

    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        fClient.disconnect();

        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_backButtonActionPerformed

    private void delButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delButtonActionPerformed
        Object[] filesToDel = fileListPane.getSelectedValues();
        boolean delSuccess;

        for(int i=0; i<filesToDel.length; i++){
            delSuccess = fClient.delete((String) filesToDel[i], userToken);
            if(delSuccess){
                System.out.println("File " + filesToDel[i] + "successfully deleted");
            }else{
                System.err.println("File " + filesToDel[i] + "delete failed");
            }
        }
    }//GEN-LAST:event_delButtonActionPerformed

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        fClient.connect(server, port);

        ArrayList<String> fileList = (ArrayList<String>) fClient.listFiles(userToken);
        fileListPane.setListData(fileList.toArray());
    }//GEN-LAST:event_refreshButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backButton;
    private javax.swing.JButton delButton;
    private javax.swing.JButton downButton;
    private javax.swing.JList fileListPane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList jList1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton refreshButton;
    // End of variables declaration//GEN-END:variables

}
