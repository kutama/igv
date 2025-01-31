/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.broad.igv.prefs;

import com.jidesoft.dialog.ButtonPanel;
import org.apache.log4j.Logger;
import org.broad.igv.DirectoryManager;
import org.broad.igv.Globals;
import org.broad.igv.batch.CommandListener;
import org.broad.igv.data.expression.ProbeToLocusMap;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.google.OAuthUtils;
import org.broad.igv.sam.AlignmentTrack.ShadeBasesOption;
import org.broad.igv.ui.FontManager;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.IGVMenuBar;
import org.broad.igv.ui.Main;
import org.broad.igv.ui.color.ColorChooserPanel;
import org.broad.igv.ui.color.ColorUtilities;
import org.broad.igv.ui.color.PaletteColorTable;
import org.broad.igv.ui.legend.MutationColorMapEditor;
import org.broad.igv.ui.util.FileDialogUtils;
import org.broad.igv.ui.util.FontChooser;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.ui.util.UIUtilities;
import org.broad.igv.util.HttpUtils;
import org.broad.igv.util.Utilities;
import org.broad.igv.util.collections.CollUtils;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.broad.igv.prefs.Constants.*;

/**
 * @author jrobinso
 */
public class PreferencesEditor extends javax.swing.JDialog {

    static Logger log = Logger.getLogger(PreferencesEditor.class);

    private boolean canceled = false;
    Map<String, String> updatedPreferenceMap = Collections.synchronizedMap(new HashMap<String, String>() {
        @Override
        public String put(String k, String v) {
            String oldValue = prefMgr.get(k);
            if ((v == null && oldValue != null) || !v.equals(oldValue)) {
                return super.put(k, v);
            }
            return v;
        }
    });
    IGVPreferences prefMgr = PreferencesManager.getPreferences();
    boolean updateOverlays = false;
    boolean inputValidated = true;
    private static int lastSelectedIndex = 0;
    boolean proxySettingsChanged = false;
    boolean tooltipSettingsChanged = false;
    private File newIGVDirectory;
    private File newCramCacheDirectory;
    private File cramCacheDirectory;

    public PreferencesEditor(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        initValues();

        tabbedPane.setSelectedIndex(lastSelectedIndex);

        // Conditionally remove database panel
        if (!prefMgr.getAsBoolean(DB_ENABLED)) {
            int idx = tabbedPane.indexOfTab("Database");
            if (idx > 0) {
                tabbedPane.remove(idx);
            }
        }

        setLocationRelativeTo(parent);
        //getRootPane().setDefaultButton(okButton);
    }



    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        canceled = true;
        setVisible(false);
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if (inputValidated) {

            checkForVCFColors();

            checkForProbeChanges();

            lastSelectedIndex = tabbedPane.getSelectedIndex();

            // Store the changed preferences
            prefMgr.putAll(updatedPreferenceMap);

            if (updatedPreferenceMap.containsKey(PORT_ENABLED) ||
                    updatedPreferenceMap.containsKey(PORT_NUMBER)) {
                CommandListener.halt();
                if (enablePortCB.isSelected()) {
                    int port = Integer.parseInt(updatedPreferenceMap.get(PORT_NUMBER));
                    CommandListener.start(port);
                }
            }


            // Overlays
            if (updateOverlays) {
                IGV.getInstance().resetOverlayTracks();
            }

            // Proxies
            if (proxySettingsChanged) {
                HttpUtils.getInstance().updateProxySettings();
            }

            // IGV directory
            if (newIGVDirectory != null) {
                moveIGVDirectory();

            }

            if(newCramCacheDirectory != null) {
                moveCramCacheDirectory();
            }

            // Tooltip
            if (tooltipSettingsChanged) {
                Main.updateTooltipSettings();
            }

            if (updatedPreferenceMap.containsKey(ENABLE_GOOGLE_MENU)) {
                IGVMenuBar.getInstance().enableGoogleMenu(Boolean.valueOf(updatedPreferenceMap.get(ENABLE_GOOGLE_MENU)));
            }


            if (updatedPreferenceMap.containsKey(SAVE_GOOGLE_CREDENTIALS)) {
                try {
                    OAuthUtils.getInstance().updateSaveOption(Boolean.valueOf(updatedPreferenceMap.get(SAVE_GOOGLE_CREDENTIALS)));
                } catch (IOException e) {
                    log.error("Error saving oauth token: " + e.getMessage());
                }
            }

            updatedPreferenceMap.clear();
            IGV.getInstance().doRefresh();
            setVisible(false);
        } else {
            resetValidation();
        }
    }


    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    private void initComponents() {
        panel7 = new JScrollPane();
        panel6 = new JPanel();
        tabbedPane = new JTabbedPane();
        panel22 = new JScrollPane();
        generalPanel = new JPanel();
        vSpacer7 = new JPanel(null);
        jPanel10 = new JPanel();
        sessionPathsCB = new JCheckBox();
        missingDataExplanation = new JLabel();
        showDefaultTrackAttributesCB = new JCheckBox();
        combinePanelsCB = new JCheckBox();
        showAttributesDisplayCheckBox = new JCheckBox();
        searchZoomCB = new JCheckBox();
        label4 = new JLabel();
        geneListFlankingField = new JTextField();
        zoomToFeatureExplanation2 = new JLabel();
        label6 = new JLabel();
        seqResolutionThreshold = new JTextField();
        label10 = new JLabel();
        fontChangeButton = new JButton();
        showRegionBoundariesCB = new JCheckBox();
        label7 = new JLabel();
        backgroundColorPanel = new JPanel();
        enableGoogleCB = new JCheckBox();
        label33 = new JLabel();
        saveGoogleCredentialsCB = new JCheckBox();
        label34 = new JLabel();
        textField1 = new JLabel();
        featureVisibilityWindowField = new JTextField();
        zoomToFeatureExplanation3 = new JLabel();
        defaultFontField = new JTextField();
        resetFontButton = new JButton();
        scaleFontsCB = new JCheckBox();
        label8 = new JLabel();
        resetBackgroundButton = new JButton();
        panel23 = new JScrollPane();
        tracksPanel = new JPanel();
        vSpacer1 = new JPanel(null);
        jLabel5 = new JLabel();
        defaultChartTrackHeightField = new JTextField();
        trackNameAttributeLabel = new JLabel();
        trackNameAttributeField = new JTextField();
        jLabel8 = new JLabel();
        defaultTrackHeightField = new JTextField();
        hSpacer1 = new JPanel(null);
        expandCB = new JCheckBox();
        normalizeCoverageCB = new JCheckBox();
        missingDataExplanation8 = new JLabel();
        panel24 = new JScrollPane();
        overlaysPanel = new JPanel();
        jPanel5 = new JPanel();
        jLabel3 = new JLabel();
        overlayAttributeTextField = new JTextField();
        overlayTrackCB = new JCheckBox();
        jLabel2 = new JLabel();
        jLabel4 = new JLabel();
        colorCodeMutationsCB = new JCheckBox();
        chooseMutationColorsButton = new JButton();
        label11 = new JLabel();
        showOrphanedMutationsCB = new JCheckBox();
        label12 = new JLabel();
        panel33 = new JPanel();
        label36 = new JLabel();
        homRefColorChooser = new ColorChooserPanel();
        label38 = new JLabel();
        homVarColorChooser = new ColorChooserPanel();
        label37 = new JLabel();
        hetVarColorChooser = new ColorChooserPanel();
        label40 = new JLabel();
        noCallColorChooser = new ColorChooserPanel();
        label41 = new JLabel();
        afRefColorChooser = new ColorChooserPanel();
        label42 = new JLabel();
        afVarColorChooser = new ColorChooserPanel();
        resetVCFButton = new JButton();
        panel35 = new JPanel();
        label43 = new JLabel();
        alleleFreqRB = new JRadioButton();
        alleleFractionRB = new JRadioButton();
        panel25 = new JScrollPane();
        chartPanel = new JPanel();
        jPanel4 = new JPanel();
        topBorderCB = new JCheckBox();
        label1 = new JLabel();
        chartDrawTrackNameCB = new JCheckBox();
        bottomBorderCB = new JCheckBox();
        jLabel7 = new JLabel();
        colorBordersCB = new JCheckBox();
        labelYAxisCB = new JCheckBox();
        autoscaleCB = new JCheckBox();
        jLabel9 = new JLabel();
        showDatarangeCB = new JCheckBox();
        panel1 = new JPanel();
        label13 = new JLabel();
        showAllHeatmapFeauresCB = new JCheckBox();
        label14 = new JLabel();
        panel20 = new JScrollPane();
        alignmentPanel = new JPanel();
        jPanel11 = new JPanel();
        panel32 = new JPanel();
        label39 = new JLabel();
        showAlignmentTrackCB = new JCheckBox();
        showCovTrackCB = new JCheckBox();
        showJunctionTrackCB = new JCheckBox();
        jPanel12 = new JPanel();
        panel13 = new JPanel();
        panel31 = new JPanel();
        jLabel11 = new JLabel();
        samMaxWindowSizeField = new JTextField();
        jLabel12 = new JLabel();
        panel4 = new JPanel();
        downsampleReadsCB = new JCheckBox();
        hSpacer3 = new JPanel(null);
        label23 = new JLabel();
        samDownsampleCountField = new JTextField();
        jLabel13 = new JLabel();
        samSamplingWindowField = new JTextField();
        panel11 = new JPanel();
        samShadeMismatchedBaseCB = new JCheckBox();
        samMinBaseQualityField = new JTextField();
        label2 = new JLabel();
        samMaxBaseQualityField = new JTextField();
        panel12 = new JPanel();
        jLabel15 = new JLabel();
        mappingQualityThresholdField = new JTextField();
        panel10 = new JPanel();
        samFlagIndelsCB = new JCheckBox();
        samFlagIndelsThresholdField = new JTextField();
        label31 = new JLabel();
        panel10clip = new JPanel();
        samFlagClippingCB = new JCheckBox();
        samFlagClippingThresholdField = new JTextField();
        label31clip = new JLabel();
        panel9 = new JPanel();
        hideIndelsBasesCB = new JCheckBox();
        hideIndelsBasesField = new JTextField();
        label45 = new JLabel();
        panel8 = new JPanel();
        samFilterDuplicatesCB = new JCheckBox();
        samFlagUnmappedPairCB = new JCheckBox();
        filterFailedReadsCB = new JCheckBox();
        showSoftClippedCB = new JCheckBox();
        filterSecondaryAlignmentsCB = new JCheckBox();
        quickConsensusModeCB = new JCheckBox();
        showCenterLineCB = new JCheckBox();
        filterSupplementaryAlignmentsCB = new JCheckBox();
        panel31b = new JPanel();
        jLabel11b = new JLabel();
        samHiddenTagsField = new JTextField();
        vSpacer5 = new JPanel(null);
        panel34 = new JPanel();
        panel5 = new JPanel();
        jLabel26 = new JLabel();
        snpThresholdField = new JTextField();
        hSpacer2 = new JPanel(null);
        useAlleleQualityCB = new JCheckBox();
        panel3 = new JPanel();
        showJunctionFlankingRegionsCB = new JCheckBox();
        label15 = new JLabel();
        junctionFlankingTextField = new JTextField();
        label16 = new JLabel();
        junctionCoverageTextField = new JTextField();
        vSpacer6 = new JPanel(null);
        panel2 = new JPanel();
        panel19 = new JPanel();
        panel16 = new JPanel();
        label9 = new JLabel();
        jLabel20 = new JLabel();
        insertSizeMinThresholdField = new JTextField();
        jLabel17 = new JLabel();
        insertSizeThresholdField = new JTextField();
        panel15 = new JPanel();
        isizeComputeCB = new JCheckBox();
        jLabel30 = new JLabel();
        insertSizeMinPercentileField = new JTextField();
        jLabel18 = new JLabel();
        insertSizeMaxPercentileField = new JTextField();
        panel26 = new JScrollPane();
        expressionPane = new JPanel();
        jPanel8 = new JPanel();
        panel18 = new JPanel();
        jLabel24 = new JLabel();
        jLabel21 = new JLabel();
        expMapToLociCB = new JRadioButton();
        expMapToGeneCB = new JRadioButton();
        panel17 = new JPanel();
        useProbeMappingCB = new JCheckBox();
        label22 = new JLabel();
        panel14 = new JPanel();
        probeMappingFileTextField = new JTextField();
        probeMappingBrowseButton = new JButton();
        panel27 = new JScrollPane();
        proxyPanel = new JPanel();
        jPanel15 = new JPanel();
        label3 = new JLabel();
        clearProxySettingsButton = new JButton();
        proxyUsernameField = new JTextField();
        jLabel28 = new JLabel();
        authenticateProxyCB = new JCheckBox();
        jLabel29 = new JLabel();
        proxyPasswordField = new JPasswordField();
        proxyHostField = new JTextField();
        proxyPortField = new JTextField();
        jLabel27 = new JLabel();
        jLabel23 = new JLabel();
        useProxyCB = new JCheckBox();
        proxyTypeCB = new JComboBox<>();
        label27 = new JLabel();
        label35 = new JLabel();
        proxyWhitelistTextArea = new JTextField();
        panel30 = new JScrollPane();
        dbPanel = new JPanel();
        label20 = new JLabel();
        panel21 = new JPanel();
        label17 = new JLabel();
        label19 = new JLabel();
        dbNameField = new JTextField();
        dbHostField = new JTextField();
        label18 = new JLabel();
        dbPortField = new JTextField();
        panel29 = new JScrollPane();
        advancedPanel = new JPanel();
        clearGenomeCacheButton = new JButton();
        enablePortCB = new JCheckBox();
        portField = new JTextField();
        jLabel22 = new JLabel();
        vSpacer12 = new JPanel(null);
        genomeUpdateCB = new JCheckBox();
        jLabel6 = new JLabel();
        dataServerURLTextField = new JTextField();
        jLabel1 = new JLabel();
        genomeServerURLTextField = new JTextField();
        editServerPropertiesCB = new JCheckBox();
        jButton1 = new JButton();
        vSpacer11 = new JPanel(null);
        autoFileDisoveryCB = new JCheckBox();
        igvDirectoryButton = new JButton();
        igvDirectoryField = new JLabel();
        label21 = new JLabel();
        tooltipOptionsPanel = new JPanel();
        label24 = new JLabel();
        label25 = new JLabel();
        label26 = new JLabel();
        toolTipInitialDelayField = new JTextField();
        tooltipReshowDelayField = new JTextField();
        tooltipDismissDelayField = new JTextField();
        antialiasingCB = new JCheckBox();
        label5 = new JLabel();
        blatURLField = new JTextField();
        vSpacer8 = new JPanel(null);
        vSpacer9 = new JPanel(null);
        vSpacer10 = new JPanel(null);
        panel36 = new JScrollPane();
        cramPanel = new JPanel();
        panel28 = new JPanel();
        panel37 = new JPanel();
        label28 = new JLabel();
        cramCacheSizeField = new JTextField();
        panel38 = new JPanel();
        label29 = new JLabel();
        cramCacheDirectoryField = new JTextField();
        cramCacheDirectoryButton = new JButton();
        cramCacheReferenceCB = new JCheckBox();
        okCancelButtonPanel = new ButtonPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== panel7 ========
        {

            //======== panel6 ========
            {
                panel6.setLayout(new BorderLayout());

                //======== tabbedPane ========
                {
                    tabbedPane.setPreferredSize(new Dimension(805, 800));
                    tabbedPane.setMaximumSize(new Dimension(805, 800));

                    //======== panel22 ========
                    {

                        //======== generalPanel ========
                        {
                            generalPanel.setLayout(new BorderLayout());

                            //---- vSpacer7 ----
                            vSpacer7.setPreferredSize(new Dimension(10, 20));
                            generalPanel.add(vSpacer7, BorderLayout.NORTH);

                            //======== jPanel10 ========
                            {
                                jPanel10.setBorder(null);
                                jPanel10.setLayout(new GridBagLayout());
                                ((GridBagLayout)jPanel10.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                ((GridBagLayout)jPanel10.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                ((GridBagLayout)jPanel10.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
                                ((GridBagLayout)jPanel10.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                //---- sessionPathsCB ----
                                sessionPathsCB.setText("Use relative paths in sessions");
                                sessionPathsCB.addActionListener(e -> sessionPathsCBActionPerformed(e));
                                jPanel10.add(sessionPathsCB, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- missingDataExplanation ----
                                missingDataExplanation.setFont(new Font("Lucida Grande", Font.ITALIC, 12));
                                missingDataExplanation.setText("(NAME, DATA_TYPE, and DATA_FILE).");
                                jPanel10.add(missingDataExplanation, new GridBagConstraints(3, 4, 5, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- showDefaultTrackAttributesCB ----
                                showDefaultTrackAttributesCB.setText("Show default track attributes");
                                showDefaultTrackAttributesCB.setToolTipText("Display default track attributes (NAME, DATA_TYPE, and DATA_FILE) in the attribute panel.");
                                showDefaultTrackAttributesCB.addActionListener(e -> showDefaultTrackAttributesCBActionPerformed(e));
                                jPanel10.add(showDefaultTrackAttributesCB, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- combinePanelsCB ----
                                combinePanelsCB.setText("Display all tracks in a single panel");
                                combinePanelsCB.addActionListener(e -> combinePanelsCBActionPerformed(e));
                                jPanel10.add(combinePanelsCB, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- showAttributesDisplayCheckBox ----
                                showAttributesDisplayCheckBox.setText("Show attribute panel");
                                showAttributesDisplayCheckBox.addActionListener(e -> showAttributesDisplayCheckBoxActionPerformed(e));
                                jPanel10.add(showAttributesDisplayCheckBox, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- searchZoomCB ----
                                searchZoomCB.setText("Zoom to features");
                                searchZoomCB.setToolTipText("This option controls the behavior of feature searches.  If true, the zoom level is changed as required to size the view to the feature size.  If false, the zoom level is unchanged.");
                                searchZoomCB.addActionListener(e -> searchZoomCBActionPerformed(e));
                                jPanel10.add(searchZoomCB, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- label4 ----
                                label4.setText("Feature flanking region (bp or %): ");
                                label4.setToolTipText("Added before and after feature locus when zooming to a feature.  Also used when defining panel extents in gene/loci list views.  A negative number is interpreted as a percentage.");
                                jPanel10.add(label4, new GridBagConstraints(0, 9, 4, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 10, 15, 5), 0, 0));

                                //---- geneListFlankingField ----
                                geneListFlankingField.setToolTipText("Added before and after feature locus when zooming to a feature.  Also used when defining panel extents in gene/loci list views.  A negative number is interpreted as a percentage.");
                                geneListFlankingField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        geneListFlankingFieldFocusLost(e);
                                    }
                                });
                                geneListFlankingField.addActionListener(e -> geneListFlankingFieldActionPerformed(e));
                                jPanel10.add(geneListFlankingField, new GridBagConstraints(4, 9, 3, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- zoomToFeatureExplanation2 ----
                                zoomToFeatureExplanation2.setFont(new Font("Lucida Grande", Font.ITALIC, 12));
                                zoomToFeatureExplanation2.setText("<html><i>&lt; 0 is interpreted as a percentage.</b>");
                                zoomToFeatureExplanation2.setVerticalAlignment(SwingConstants.TOP);
                                jPanel10.add(zoomToFeatureExplanation2, new GridBagConstraints(7, 9, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- label6 ----
                                label6.setText("Sequence resolution threshold (bp/pixel):");
                                jPanel10.add(label6, new GridBagConstraints(0, 11, 4, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 10, 15, 5), 0, 0));

                                //---- seqResolutionThreshold ----
                                seqResolutionThreshold.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        seqResolutionThresholdFocusLost(e);
                                    }
                                });
                                seqResolutionThreshold.addActionListener(e -> seqResolutionThresholdActionPerformed(e));
                                jPanel10.add(seqResolutionThreshold, new GridBagConstraints(4, 11, 3, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- label10 ----
                                label10.setText("Default font:");
                                label10.setLabelFor(defaultFontField);
                                jPanel10.add(label10, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 10, 15, 5), 0, 0));

                                //---- fontChangeButton ----
                                fontChangeButton.setText("Change...");
                                fontChangeButton.addActionListener(e -> fontChangeButtonActionPerformed(e));
                                jPanel10.add(fontChangeButton, new GridBagConstraints(6, 12, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- showRegionBoundariesCB ----
                                showRegionBoundariesCB.setText("Show region boundaries");
                                showRegionBoundariesCB.addActionListener(e -> showRegionBoundariesCBActionPerformed(e));
                                jPanel10.add(showRegionBoundariesCB, new GridBagConstraints(0, 5, 4, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- label7 ----
                                label7.setText("Background color (click to change):");
                                jPanel10.add(label7, new GridBagConstraints(0, 14, 3, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 10, 15, 5), 0, 0));

                                //======== backgroundColorPanel ========
                                {
                                    backgroundColorPanel.setPreferredSize(new Dimension(20, 20));
                                    backgroundColorPanel.setBorder(new BevelBorder(BevelBorder.RAISED));
                                    backgroundColorPanel.addMouseListener(new MouseAdapter() {
                                        @Override
                                        public void mouseClicked(MouseEvent e) {
                                            backgroundColorPanelMouseClicked(e);
                                        }
                                    });
                                    backgroundColorPanel.setLayout(null);
                                }
                                jPanel10.add(backgroundColorPanel, new GridBagConstraints(3, 14, 2, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- enableGoogleCB ----
                                enableGoogleCB.setText("Enable Google access");
                                enableGoogleCB.addActionListener(e -> enableGoogleCBActionPerformed(e));
                                jPanel10.add(enableGoogleCB, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- label33 ----
                                label33.setText("Enable loading from Google apis.");
                                label33.setFont(new Font("Lucida Grande", Font.ITALIC, 13));
                                jPanel10.add(label33, new GridBagConstraints(3, 7, 5, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- saveGoogleCredentialsCB ----
                                saveGoogleCredentialsCB.setText("Save Google credentials");
                                saveGoogleCredentialsCB.addActionListener(e -> saveGoogleCredentialsCBActionPerformed(e));
                                jPanel10.add(saveGoogleCredentialsCB, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- label34 ----
                                label34.setText("Save authorization credentials across sessions");
                                label34.setFont(new Font("Lucida Grande", Font.ITALIC, 13));
                                jPanel10.add(label34, new GridBagConstraints(3, 8, 5, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- textField1 ----
                                textField1.setText("Default visibility window (kilobases):");
                                textField1.setToolTipText("A value > 0 will set a default threshold windows size in kilobases above which features from indexed files are not loaded.  The threshold (\"visibility window\") can be overridden explicitly for individual tracks via the track menu.");
                                jPanel10.add(textField1, new GridBagConstraints(0, 10, 3, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 10, 15, 5), 0, 0));

                                //---- featureVisibilityWindowField ----
                                featureVisibilityWindowField.setToolTipText("A value > 0 will set a default threshold windows size in kilobases above which features from indexed files are not loaded.  The threshold (\"visibility window\") can be overridden explicitly for individual tracks via the track menu.");
                                featureVisibilityWindowField.addActionListener(e -> featureVisibilityWindowFieldActionPerformed(e));
                                featureVisibilityWindowField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        featureVisibilityWindowFieldFocusLost(e);
                                    }
                                });
                                jPanel10.add(featureVisibilityWindowField, new GridBagConstraints(4, 10, 3, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- zoomToFeatureExplanation3 ----
                                zoomToFeatureExplanation3.setFont(new Font("Lucida Grande", Font.ITALIC, 12));
                                zoomToFeatureExplanation3.setText("<html><i>&lt; 0 disables visibility window.</b>");
                                zoomToFeatureExplanation3.setVerticalAlignment(SwingConstants.TOP);
                                jPanel10.add(zoomToFeatureExplanation3, new GridBagConstraints(7, 10, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- defaultFontField ----
                                defaultFontField.setEditable(false);
                                jPanel10.add(defaultFontField, new GridBagConstraints(1, 12, 4, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- resetFontButton ----
                                resetFontButton.setText("Reset to default");
                                resetFontButton.addActionListener(e -> resetFontButtonActionPerformed(e));
                                jPanel10.add(resetFontButton, new GridBagConstraints(7, 12, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- scaleFontsCB ----
                                scaleFontsCB.setText("Scale fonts");
                                scaleFontsCB.addActionListener(e -> scaleFontsCBActionPerformed(e));
                                jPanel10.add(scaleFontsCB, new GridBagConstraints(0, 13, 2, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));

                                //---- label8 ----
                                label8.setText("<html><i>Scale fonts for high resolution screens.  Requires restart.");
                                jPanel10.add(label8, new GridBagConstraints(2, 13, 7, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 0), 0, 0));

                                //---- resetBackgroundButton ----
                                resetBackgroundButton.setText("Reset to default");
                                resetBackgroundButton.addActionListener(e -> resetBackgroundButtonActionPerformed(e));
                                jPanel10.add(resetBackgroundButton, new GridBagConstraints(7, 14, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 15, 5), 0, 0));
                            }
                            generalPanel.add(jPanel10, BorderLayout.CENTER);
                        }
                        panel22.setViewportView(generalPanel);
                    }
                    tabbedPane.addTab("General", panel22);

                    //======== panel23 ========
                    {

                        //======== tracksPanel ========
                        {
                            tracksPanel.setMinimumSize(new Dimension(700, 407));
                            tracksPanel.setLayout(new GridBagLayout());
                            ((GridBagLayout)tracksPanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
                            ((GridBagLayout)tracksPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                            ((GridBagLayout)tracksPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
                            ((GridBagLayout)tracksPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
                            tracksPanel.add(vSpacer1, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 10), 0, 0));

                            //---- jLabel5 ----
                            jLabel5.setText("Default Track Height, Charts (Pixels)");
                            tracksPanel.add(jLabel5, new GridBagConstraints(2, 1, 2, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 10), 0, 0));

                            //---- defaultChartTrackHeightField ----
                            defaultChartTrackHeightField.setText("40");
                            defaultChartTrackHeightField.setMinimumSize(new Dimension(60, 28));
                            defaultChartTrackHeightField.setToolTipText("Default height of chart tracks (barcharts, scatterplots, etc)");
                            defaultChartTrackHeightField.addActionListener(e -> defaultChartTrackHeightFieldActionPerformed(e));
                            defaultChartTrackHeightField.addFocusListener(new FocusAdapter() {
                                @Override
                                public void focusLost(FocusEvent e) {
                                    defaultChartTrackHeightFieldFocusLost(e);
                                }
                            });
                            tracksPanel.add(defaultChartTrackHeightField, new GridBagConstraints(4, 1, 2, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 10), 0, 0));

                            //---- trackNameAttributeLabel ----
                            trackNameAttributeLabel.setText("Track Name Attribute");
                            trackNameAttributeLabel.setToolTipText("Name of an attribute to be used to label tracks.  If provided, tracks will be labeled with the corresponding attribute values from the sample information file");
                            tracksPanel.add(trackNameAttributeLabel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 10), 0, 0));

                            //---- trackNameAttributeField ----
                            trackNameAttributeField.addActionListener(e -> trackNameAttributeFieldActionPerformed(e));
                            trackNameAttributeField.addFocusListener(new FocusAdapter() {
                                @Override
                                public void focusLost(FocusEvent e) {
                                    trackNameAttributeFieldFocusLost(e);
                                }
                            });
                            tracksPanel.add(trackNameAttributeField, new GridBagConstraints(3, 3, 3, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 10), 0, 0));

                            //---- jLabel8 ----
                            jLabel8.setText("Default Track Height, Other (Pixels)");
                            tracksPanel.add(jLabel8, new GridBagConstraints(2, 2, 2, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 10), 0, 0));

                            //---- defaultTrackHeightField ----
                            defaultTrackHeightField.setText("15");
                            defaultTrackHeightField.setMinimumSize(new Dimension(60, 28));
                            defaultTrackHeightField.addActionListener(e -> defaultTrackHeightFieldActionPerformed(e));
                            defaultTrackHeightField.addFocusListener(new FocusAdapter() {
                                @Override
                                public void focusLost(FocusEvent e) {
                                    defaultTrackHeightFieldFocusLost(e);
                                }
                            });
                            tracksPanel.add(defaultTrackHeightField, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 10), 0, 0));
                            tracksPanel.add(hSpacer1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 10), 0, 0));

                            //---- expandCB ----
                            expandCB.setText("Expand Feature Tracks");
                            expandCB.addActionListener(e -> expandCBActionPerformed(e));
                            tracksPanel.add(expandCB, new GridBagConstraints(2, 5, 2, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 10), 0, 0));

                            //---- normalizeCoverageCB ----
                            normalizeCoverageCB.setText("Normalize Coverage Data");
                            normalizeCoverageCB.addActionListener(e -> normalizeCoverageCBActionPerformed(e));
                            normalizeCoverageCB.addFocusListener(new FocusAdapter() {
                                @Override
                                public void focusLost(FocusEvent e) {
                                    normalizeCoverageCBFocusLost(e);
                                }
                            });
                            tracksPanel.add(normalizeCoverageCB, new GridBagConstraints(2, 6, 2, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 10), 0, 0));

                            //---- missingDataExplanation8 ----
                            missingDataExplanation8.setFont(new Font("Lucida Grande", Font.ITALIC, 12));
                            missingDataExplanation8.setText("<html><i> Applies to coverage tracks computed with igvtools (.tdf files).  If selected, coverage values are scaled by (1,000,000 / totalCount),  where totalCount is the total number of features or alignments.");
                            missingDataExplanation8.setMaximumSize(new Dimension(500, 2147483647));
                            missingDataExplanation8.setPreferredSize(new Dimension(500, 50));
                            tracksPanel.add(missingDataExplanation8, new GridBagConstraints(2, 7, 6, 2, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 15, 0), 0, 0));
                        }
                        panel23.setViewportView(tracksPanel);
                    }
                    tabbedPane.addTab("Tracks", panel23);

                    //======== panel24 ========
                    {

                        //======== overlaysPanel ========
                        {
                            overlaysPanel.setLayout(null);

                            //======== jPanel5 ========
                            {
                                jPanel5.setBorder(new TitledBorder("MAF Somatic Mutations"));
                                jPanel5.setLayout(null);

                                //---- jLabel3 ----
                                jLabel3.setText("Linking attribute column:");
                                jPanel5.add(jLabel3);
                                jLabel3.setBounds(new Rectangle(new Point(65, 86), jLabel3.getPreferredSize()));

                                //---- overlayAttributeTextField ----
                                overlayAttributeTextField.setText("LINKING_ID");
                                overlayAttributeTextField.addActionListener(e -> overlayAttributeTextFieldActionPerformed(e));
                                overlayAttributeTextField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        overlayAttributeTextFieldFocusLost(e);
                                    }
                                });
                                jPanel5.add(overlayAttributeTextField);
                                overlayAttributeTextField.setBounds(240, 80, 228, overlayAttributeTextField.getPreferredSize().height);

                                //---- overlayTrackCB ----
                                overlayTrackCB.setSelected(true);
                                overlayTrackCB.setText("Overlay mutation tracks");
                                overlayTrackCB.setActionCommand("overlayTracksCB");
                                overlayTrackCB.addActionListener(e -> overlayTrackCBActionPerformed(e));
                                jPanel5.add(overlayTrackCB);
                                overlayTrackCB.setBounds(new Rectangle(new Point(6, 51), overlayTrackCB.getPreferredSize()));

                                //---- jLabel2 ----
                                jLabel2.setFont(new Font("Lucida Grande", Font.ITALIC, 12));
                                jPanel5.add(jLabel2);
                                jLabel2.setBounds(new Rectangle(new Point(6, 6), jLabel2.getPreferredSize()));

                                //---- jLabel4 ----
                                jLabel4.setFont(new Font("Lucida Grande", Font.ITALIC, 12));
                                jPanel5.add(jLabel4);
                                jLabel4.setBounds(new Rectangle(new Point(6, 12), jLabel4.getPreferredSize()));

                                //---- colorCodeMutationsCB ----
                                colorCodeMutationsCB.setText("Color code mutations");
                                colorCodeMutationsCB.addActionListener(e -> colorMutationsCBActionPerformed(e));
                                jPanel5.add(colorCodeMutationsCB);
                                colorCodeMutationsCB.setBounds(new Rectangle(new Point(0, 295), colorCodeMutationsCB.getPreferredSize()));

                                //---- chooseMutationColorsButton ----
                                chooseMutationColorsButton.setText("Choose colors");
                                chooseMutationColorsButton.setFont(UIManager.getFont("Button.font"));
                                chooseMutationColorsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
                                chooseMutationColorsButton.addActionListener(e -> chooseMutationColorsButtonActionPerformed(e));
                                jPanel5.add(chooseMutationColorsButton);
                                chooseMutationColorsButton.setBounds(new Rectangle(new Point(185, 292), chooseMutationColorsButton.getPreferredSize()));

                                //---- label11 ----
                                label11.setText("<html><i>Name of a sample attribute column to link mutation and data tracks");
                                label11.setVerticalAlignment(SwingConstants.TOP);
                                jPanel5.add(label11);
                                label11.setBounds(110, 115, 360, 50);

                                //---- showOrphanedMutationsCB ----
                                showOrphanedMutationsCB.setText("Show orphaned mutation tracks");
                                showOrphanedMutationsCB.addActionListener(e -> showOrphanedMutationsCBActionPerformed(e));
                                jPanel5.add(showOrphanedMutationsCB);
                                showOrphanedMutationsCB.setBounds(new Rectangle(new Point(70, 180), showOrphanedMutationsCB.getPreferredSize()));

                                //---- label12 ----
                                label12.setText("<html><i>Select to show mutation tracks with no corresponding data track.");
                                label12.setVerticalAlignment(SwingConstants.TOP);
                                jPanel5.add(label12);
                                label12.setBounds(110, 210, 360, 55);

                                { // compute preferred size
                                    Dimension preferredSize = new Dimension();
                                    for(int i = 0; i < jPanel5.getComponentCount(); i++) {
                                        Rectangle bounds = jPanel5.getComponent(i).getBounds();
                                        preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                        preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                    }
                                    Insets insets = jPanel5.getInsets();
                                    preferredSize.width += insets.right;
                                    preferredSize.height += insets.bottom;
                                    jPanel5.setMinimumSize(preferredSize);
                                    jPanel5.setPreferredSize(preferredSize);
                                }
                            }
                            overlaysPanel.add(jPanel5);
                            jPanel5.setBounds(20, 365, 690, 344);

                            //======== panel33 ========
                            {
                                panel33.setBorder(new TitledBorder("VCF Variant Colors"));
                                panel33.setLayout(null);

                                //---- label36 ----
                                label36.setText("Homozygous reference");
                                panel33.add(label36);
                                label36.setBounds(6, 22, 339, 29);
                                panel33.add(homRefColorChooser);
                                homRefColorChooser.setBounds(345, 22, 55, 29);

                                //---- label38 ----
                                label38.setText("Homozygous variant");
                                panel33.add(label38);
                                label38.setBounds(6, 51, 339, 29);
                                panel33.add(homVarColorChooser);
                                homVarColorChooser.setBounds(345, 51, 55, 29);

                                //---- label37 ----
                                label37.setText("Heterozygous variant");
                                panel33.add(label37);
                                label37.setBounds(6, 80, 339, 29);
                                panel33.add(hetVarColorChooser);
                                hetVarColorChooser.setBounds(345, 80, 55, 29);

                                //---- label40 ----
                                label40.setText("No call");
                                panel33.add(label40);
                                label40.setBounds(6, 109, 339, 29);
                                panel33.add(noCallColorChooser);
                                noCallColorChooser.setBounds(345, 109, 55, 29);

                                //---- label41 ----
                                label41.setText("Allele freq - reference");
                                panel33.add(label41);
                                label41.setBounds(6, 138, 339, 29);
                                panel33.add(afRefColorChooser);
                                afRefColorChooser.setBounds(345, 138, 55, 29);

                                //---- label42 ----
                                label42.setText("Allele freq - variant");
                                panel33.add(label42);
                                label42.setBounds(6, 167, 339, 29);
                                panel33.add(afVarColorChooser);
                                afVarColorChooser.setBounds(345, 167, 55, 29);

                                //---- resetVCFButton ----
                                resetVCFButton.setText("Reset to defaults");
                                resetVCFButton.addActionListener(e -> resetVCFButtonActionPerformed(e));
                                panel33.add(resetVCFButton);
                                resetVCFButton.setBounds(5, 265, 144, resetVCFButton.getPreferredSize().height);

                                //======== panel35 ========
                                {
                                    panel35.setLayout(new FlowLayout(FlowLayout.LEFT));

                                    //---- label43 ----
                                    label43.setText("Color variant by: ");
                                    panel35.add(label43);

                                    //---- alleleFreqRB ----
                                    alleleFreqRB.setText("Allele frequency");
                                    panel35.add(alleleFreqRB);

                                    //---- alleleFractionRB ----
                                    alleleFractionRB.setText("Allele fraction");
                                    panel35.add(alleleFractionRB);
                                }
                                panel33.add(panel35);
                                panel35.setBounds(5, 220, 450, panel35.getPreferredSize().height);

                                { // compute preferred size
                                    Dimension preferredSize = new Dimension();
                                    for(int i = 0; i < panel33.getComponentCount(); i++) {
                                        Rectangle bounds = panel33.getComponent(i).getBounds();
                                        preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                        preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                    }
                                    Insets insets = panel33.getInsets();
                                    preferredSize.width += insets.right;
                                    preferredSize.height += insets.bottom;
                                    panel33.setMinimumSize(preferredSize);
                                    panel33.setPreferredSize(preferredSize);
                                }
                            }
                            overlaysPanel.add(panel33);
                            panel33.setBounds(20, 25, 690, 325);

                            { // compute preferred size
                                Dimension preferredSize = new Dimension();
                                for(int i = 0; i < overlaysPanel.getComponentCount(); i++) {
                                    Rectangle bounds = overlaysPanel.getComponent(i).getBounds();
                                    preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                    preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                }
                                Insets insets = overlaysPanel.getInsets();
                                preferredSize.width += insets.right;
                                preferredSize.height += insets.bottom;
                                overlaysPanel.setMinimumSize(preferredSize);
                                overlaysPanel.setPreferredSize(preferredSize);
                            }
                        }
                        panel24.setViewportView(overlaysPanel);
                    }
                    tabbedPane.addTab("Variants", panel24);

                    //======== panel25 ========
                    {

                        //======== chartPanel ========
                        {
                            chartPanel.setLayout(null);

                            //======== jPanel4 ========
                            {
                                jPanel4.setBorder(null);
                                jPanel4.setLayout(null);

                                //---- topBorderCB ----
                                topBorderCB.setText("Draw Top Border");
                                topBorderCB.addActionListener(e -> {
			topBorderCBActionPerformed(e);
			topBorderCBActionPerformed(e);
		});
                                jPanel4.add(topBorderCB);
                                topBorderCB.setBounds(new Rectangle(new Point(30, 36), topBorderCB.getPreferredSize()));

                                //---- label1 ----
                                label1.setFont(label1.getFont());
                                label1.setText("<html><b>Default settings for barcharts and scatterplots:");
                                jPanel4.add(label1);
                                label1.setBounds(10, 10, 380, 25);

                                //---- chartDrawTrackNameCB ----
                                chartDrawTrackNameCB.setText("Draw Track Label");
                                chartDrawTrackNameCB.addActionListener(e -> chartDrawTrackNameCBActionPerformed(e));
                                jPanel4.add(chartDrawTrackNameCB);
                                chartDrawTrackNameCB.setBounds(new Rectangle(new Point(30, 126), chartDrawTrackNameCB.getPreferredSize()));

                                //---- bottomBorderCB ----
                                bottomBorderCB.setText("Draw Bottom Border");
                                bottomBorderCB.addActionListener(e -> bottomBorderCBActionPerformed(e));
                                jPanel4.add(bottomBorderCB);
                                bottomBorderCB.setBounds(new Rectangle(new Point(30, 66), bottomBorderCB.getPreferredSize()));

                                //---- jLabel7 ----
                                jLabel7.setText("<html><i>Dynamically rescale to the range of the data in view.");
                                jPanel4.add(jLabel7);
                                jLabel7.setBounds(220, 170, 371, 50);

                                //---- colorBordersCB ----
                                colorBordersCB.setText("Color Borders");
                                colorBordersCB.addActionListener(e -> colorBordersCBActionPerformed(e));
                                jPanel4.add(colorBordersCB);
                                colorBordersCB.setBounds(new Rectangle(new Point(30, 96), colorBordersCB.getPreferredSize()));

                                //---- labelYAxisCB ----
                                labelYAxisCB.setText("Label Y Axis");
                                labelYAxisCB.addActionListener(e -> labelYAxisCBActionPerformed(e));
                                jPanel4.add(labelYAxisCB);
                                labelYAxisCB.setBounds(new Rectangle(new Point(30, 156), labelYAxisCB.getPreferredSize()));

                                //---- autoscaleCB ----
                                autoscaleCB.setText("Continuous Autoscale");
                                autoscaleCB.addActionListener(e -> autoscaleCBActionPerformed(e));
                                jPanel4.add(autoscaleCB);
                                autoscaleCB.setBounds(new Rectangle(new Point(30, 186), autoscaleCB.getPreferredSize()));

                                //---- jLabel9 ----
                                jLabel9.setText("<html><i>Draw a label centered over the track. ");
                                jPanel4.add(jLabel9);
                                jLabel9.setBounds(220, 159, 355, jLabel9.getPreferredSize().height);

                                //---- showDatarangeCB ----
                                showDatarangeCB.setText("Show Data Range");
                                showDatarangeCB.addActionListener(e -> showDatarangeCBActionPerformed(e));
                                showDatarangeCB.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        showDatarangeCBFocusLost(e);
                                    }
                                });
                                jPanel4.add(showDatarangeCB);
                                showDatarangeCB.setBounds(30, 216, showDatarangeCB.getPreferredSize().width, 26);

                                { // compute preferred size
                                    Dimension preferredSize = new Dimension();
                                    for(int i = 0; i < jPanel4.getComponentCount(); i++) {
                                        Rectangle bounds = jPanel4.getComponent(i).getBounds();
                                        preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                        preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                    }
                                    Insets insets = jPanel4.getInsets();
                                    preferredSize.width += insets.right;
                                    preferredSize.height += insets.bottom;
                                    jPanel4.setMinimumSize(preferredSize);
                                    jPanel4.setPreferredSize(preferredSize);
                                }
                            }
                            chartPanel.add(jPanel4);
                            jPanel4.setBounds(20, 20, 650, 290);

                            //======== panel1 ========
                            {
                                panel1.setBorder(null);
                                panel1.setLayout(null);

                                //---- label13 ----
                                label13.setText("<html><b>Default settings for heatmaps:");
                                panel1.add(label13);
                                label13.setBounds(10, 5, 250, 30);

                                //---- showAllHeatmapFeauresCB ----
                                showAllHeatmapFeauresCB.setText("Show all features");
                                showAllHeatmapFeauresCB.addActionListener(e -> showAllHeatmapFeauresCBActionPerformed(e));
                                panel1.add(showAllHeatmapFeauresCB);
                                showAllHeatmapFeauresCB.setBounds(new Rectangle(new Point(20, 45), showAllHeatmapFeauresCB.getPreferredSize()));

                                //---- label14 ----
                                label14.setText("<html><i>Paint all features/segments with a minimum width of 1 pixel.  If not checked, features/segments with screen widths less than 1 pixel are not drawn.");
                                panel1.add(label14);
                                label14.setBounds(200, 35, 425, 60);
                            }
                            chartPanel.add(panel1);
                            panel1.setBounds(20, 340, 650, 135);

                            { // compute preferred size
                                Dimension preferredSize = new Dimension();
                                for(int i = 0; i < chartPanel.getComponentCount(); i++) {
                                    Rectangle bounds = chartPanel.getComponent(i).getBounds();
                                    preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                    preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                }
                                Insets insets = chartPanel.getInsets();
                                preferredSize.width += insets.right;
                                preferredSize.height += insets.bottom;
                                chartPanel.setMinimumSize(preferredSize);
                                chartPanel.setPreferredSize(preferredSize);
                            }
                        }
                        panel25.setViewportView(chartPanel);
                    }
                    tabbedPane.addTab("Charts", panel25);

                    //======== panel20 ========
                    {

                        //======== alignmentPanel ========
                        {
                            alignmentPanel.setLayout(new BoxLayout(alignmentPanel, BoxLayout.Y_AXIS));

                            //======== jPanel11 ========
                            {
                                jPanel11.setBorder(new TitledBorder("Track Display Options"));
                                jPanel11.setLayout(new FlowLayout(FlowLayout.LEFT));

                                //======== panel32 ========
                                {
                                    panel32.setLayout(new FlowLayout(FlowLayout.LEFT));

                                    //---- label39 ----
                                    label39.setText("On initial load show:");
                                    panel32.add(label39);

                                    //---- showAlignmentTrackCB ----
                                    showAlignmentTrackCB.setText("Alignment Track");
                                    showAlignmentTrackCB.setHorizontalAlignment(SwingConstants.LEFT);
                                    showAlignmentTrackCB.addActionListener(e -> showAlignmentTrackCBActionPerformed(e));
                                    panel32.add(showAlignmentTrackCB);

                                    //---- showCovTrackCB ----
                                    showCovTrackCB.setText("Coverage Track");
                                    showCovTrackCB.addActionListener(e -> showCovTrackCBActionPerformed(e));
                                    panel32.add(showCovTrackCB);

                                    //---- showJunctionTrackCB ----
                                    showJunctionTrackCB.setText("Splice Junction Track");
                                    showJunctionTrackCB.addActionListener(e -> showJunctionTrackCBActionPerformed(e));
                                    panel32.add(showJunctionTrackCB);
                                }
                                jPanel11.add(panel32);
                            }
                            alignmentPanel.add(jPanel11);

                            //======== jPanel12 ========
                            {
                                jPanel12.setBorder(new TitledBorder("Alignment Track Options"));
                                jPanel12.setLayout(new BoxLayout(jPanel12, BoxLayout.Y_AXIS));

                                //======== panel13 ========
                                {
                                    panel13.setLayout(new GridLayout(7, 0));

                                    //======== panel31 ========
                                    {
                                        panel31.setLayout(new FlowLayout(FlowLayout.LEFT));

                                        //---- jLabel11 ----
                                        jLabel11.setText("Visibility range threshold (kb):");
                                        jLabel11.setPreferredSize(new Dimension(250, 16));
                                        panel31.add(jLabel11);

                                        //---- samMaxWindowSizeField ----
                                        samMaxWindowSizeField.setText("jTextField1");
                                        samMaxWindowSizeField.setPreferredSize(new Dimension(80, 28));
                                        samMaxWindowSizeField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                samMaxWindowSizeFieldFocusLost(e);
                                            }
                                        });
                                        samMaxWindowSizeField.addActionListener(e -> samMaxWindowSizeFieldActionPerformed(e));
                                        panel31.add(samMaxWindowSizeField);

                                        //---- jLabel12 ----
                                        jLabel12.setText("<html><i>Range at which alignments become visible");
                                        panel31.add(jLabel12);
                                    }
                                    panel13.add(panel31);

                                    //======== panel4 ========
                                    {
                                        panel4.setBorder(null);
                                        panel4.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));

                                        //---- downsampleReadsCB ----
                                        downsampleReadsCB.setText("Downsample reads");
                                        downsampleReadsCB.setHorizontalAlignment(SwingConstants.LEFT);
                                        downsampleReadsCB.addActionListener(e -> downsampleReadsCBActionPerformed(e));
                                        panel4.add(downsampleReadsCB);
                                        panel4.add(hSpacer3);

                                        //---- label23 ----
                                        label23.setText("Max read count:");
                                        panel4.add(label23);

                                        //---- samDownsampleCountField ----
                                        samDownsampleCountField.setPreferredSize(new Dimension(100, 28));
                                        samDownsampleCountField.addActionListener(e -> samDownsampleCountFieldActionPerformed(e));
                                        samDownsampleCountField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                samDownsampleCountFieldFocusLost(e);
                                            }
                                        });
                                        panel4.add(samDownsampleCountField);

                                        //---- jLabel13 ----
                                        jLabel13.setText("per window size (bases):");
                                        panel4.add(jLabel13);

                                        //---- samSamplingWindowField ----
                                        samSamplingWindowField.setText("jTextField1");
                                        samSamplingWindowField.setPreferredSize(new Dimension(100, 28));
                                        samSamplingWindowField.addActionListener(e -> samSamplingWindowFieldActionPerformed(e));
                                        samSamplingWindowField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                samSamplingWindowFieldFocusLost(e);
                                            }
                                        });
                                        panel4.add(samSamplingWindowField);
                                    }
                                    panel13.add(panel4);

                                    //======== panel11 ========
                                    {
                                        panel11.setBorder(null);
                                        panel11.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));

                                        //---- samShadeMismatchedBaseCB ----
                                        samShadeMismatchedBaseCB.setText("Shade mismatched bases by quality:");
                                        samShadeMismatchedBaseCB.addActionListener(e -> samShadeMismatchedBaseCBActionPerformed(e));
                                        panel11.add(samShadeMismatchedBaseCB);

                                        //---- samMinBaseQualityField ----
                                        samMinBaseQualityField.setText("0");
                                        samMinBaseQualityField.setPreferredSize(new Dimension(60, 28));
                                        samMinBaseQualityField.addActionListener(e -> samMinBaseQualityFieldActionPerformed(e));
                                        samMinBaseQualityField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                samMinBaseQualityFieldFocusLost(e);
                                            }
                                        });
                                        panel11.add(samMinBaseQualityField);

                                        //---- label2 ----
                                        label2.setText("to");
                                        panel11.add(label2);

                                        //---- samMaxBaseQualityField ----
                                        samMaxBaseQualityField.setText("0");
                                        samMaxBaseQualityField.setPreferredSize(new Dimension(60, 28));
                                        samMaxBaseQualityField.addActionListener(e -> samMaxBaseQualityFieldActionPerformed(e));
                                        samMaxBaseQualityField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                samMaxBaseQualityFieldFocusLost(e);
                                            }
                                        });
                                        panel11.add(samMaxBaseQualityField);
                                    }
                                    panel13.add(panel11);

                                    //======== panel12 ========
                                    {
                                        panel12.setLayout(new FlowLayout(FlowLayout.LEFT));

                                        //---- jLabel15 ----
                                        jLabel15.setText("Mapping quality threshold:");
                                        panel12.add(jLabel15);

                                        //---- mappingQualityThresholdField ----
                                        mappingQualityThresholdField.setText("0");
                                        mappingQualityThresholdField.setPreferredSize(new Dimension(60, 28));
                                        mappingQualityThresholdField.addActionListener(e -> mappingQualityThresholdFieldActionPerformed(e));
                                        mappingQualityThresholdField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                mappingQualityThresholdFieldFocusLost(e);
                                            }
                                        });
                                        panel12.add(mappingQualityThresholdField);
                                    }
                                    panel13.add(panel12);

                                    //======== panel10 ========
                                    {
                                        panel10.setBorder(null);
                                        panel10.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));

                                        //---- samFlagIndelsCB ----
                                        samFlagIndelsCB.setText("Label indels > ");
                                        samFlagIndelsCB.addActionListener(e -> samFlagIndelsCBActionPerformed(e));
                                        panel10.add(samFlagIndelsCB);

                                        //---- samFlagIndelsThresholdField ----
                                        samFlagIndelsThresholdField.setPreferredSize(new Dimension(60, 26));
                                        samFlagIndelsThresholdField.addActionListener(e -> {
			samFlagIndelsThresholdFieldActionPerformed(e);
			samFlagIndelsThresholdFieldActionPerformed(e);
		});
                                        samFlagIndelsThresholdField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                samFlagIndelsThresholdFieldFocusLost(e);
                                            }
                                        });
                                        panel10.add(samFlagIndelsThresholdField);

                                        //---- label31 ----
                                        label31.setText(" bases");
                                        panel10.add(label31);
                                    }
                                    panel13.add(panel10);

                                    //======== panel10clip ========
                                    {
                                        panel10clip.setBorder(null);
                                        panel10clip.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));

                                        //---- samFlagClippingCB ----
                                        samFlagClippingCB.setText("Flag clipping > ");
                                        samFlagClippingCB.addActionListener(e -> samFlagClippingCBActionPerformed(e));
                                        panel10clip.add(samFlagClippingCB);

                                        //---- samFlagClippingThresholdField ----
                                        samFlagClippingThresholdField.setPreferredSize(new Dimension(60, 26));
                                        samFlagClippingThresholdField.addActionListener(e -> {
			samFlagClippingThresholdFieldActionPerformed(e);
			samFlagClippingThresholdFieldActionPerformed(e);
		});
                                        samFlagClippingThresholdField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                samFlagClippingThresholdFieldFocusLost(e);
                                            }
                                        });
                                        panel10clip.add(samFlagClippingThresholdField);

                                        //---- label31clip ----
                                        label31clip.setText(" bases");
                                        panel10clip.add(label31clip);
                                    }
                                    panel13.add(panel10clip);

                                    //======== panel9 ========
                                    {
                                        panel9.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));

                                        //---- hideIndelsBasesCB ----
                                        hideIndelsBasesCB.setText("Hide indels < ");
                                        hideIndelsBasesCB.addActionListener(e -> hideIndelsBasesCBActionPerformed(e));
                                        panel9.add(hideIndelsBasesCB);

                                        //---- hideIndelsBasesField ----
                                        hideIndelsBasesField.setPreferredSize(new Dimension(60, 26));
                                        hideIndelsBasesField.addActionListener(e -> hideIndelsBasesFieldActionPerformed(e));
                                        hideIndelsBasesField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                hideIndelsBasesFieldFocusLost(e);
                                            }
                                        });
                                        panel9.add(hideIndelsBasesField);

                                        //---- label45 ----
                                        label45.setText(" bases");
                                        panel9.add(label45);
                                    }
                                    panel13.add(panel9);
                                }
                                jPanel12.add(panel13);

                                //======== panel8 ========
                                {
                                    panel8.setLayout(new GridLayout(4, 1));

                                    //---- samFilterDuplicatesCB ----
                                    samFilterDuplicatesCB.setText("Filter duplicate reads");
                                    samFilterDuplicatesCB.addActionListener(e -> samShowDuplicatesCBActionPerformed(e));
                                    panel8.add(samFilterDuplicatesCB);

                                    //---- samFlagUnmappedPairCB ----
                                    samFlagUnmappedPairCB.setText("Flag unmapped pairs");
                                    samFlagUnmappedPairCB.addActionListener(e -> samFlagUnmappedPairCBActionPerformed(e));
                                    panel8.add(samFlagUnmappedPairCB);

                                    //---- filterFailedReadsCB ----
                                    filterFailedReadsCB.setText("Filter vendor failed reads");
                                    filterFailedReadsCB.addActionListener(e -> filterVendorFailedReadsCBActionPerformed(e));
                                    panel8.add(filterFailedReadsCB);

                                    //---- showSoftClippedCB ----
                                    showSoftClippedCB.setText("Show soft-clipped bases");
                                    showSoftClippedCB.addActionListener(e -> showSoftClippedCBActionPerformed(e));
                                    panel8.add(showSoftClippedCB);

                                    //---- filterSecondaryAlignmentsCB ----
                                    filterSecondaryAlignmentsCB.setText("Filter secondary alignments");
                                    filterSecondaryAlignmentsCB.addActionListener(e -> filterSecondaryAlignmentsCBActionPerformed(e));
                                    panel8.add(filterSecondaryAlignmentsCB);

                                    //---- quickConsensusModeCB ----
                                    quickConsensusModeCB.setText("Quick consensus mode");
                                    quickConsensusModeCB.addActionListener(e -> quickConsensusModeCBActionPerformed());
                                    panel8.add(quickConsensusModeCB);

                                    //---- showCenterLineCB ----
                                    showCenterLineCB.setText("Show center line");
                                    showCenterLineCB.addActionListener(e -> showCenterLineCBActionPerformed(e));
                                    panel8.add(showCenterLineCB);

                                    //---- filterSupplementaryAlignmentsCB ----
                                    filterSupplementaryAlignmentsCB.setText("Filter supplementary alignments");
                                    filterSupplementaryAlignmentsCB.addActionListener(e -> filterSupplementaryAlignmentsCBActionPerformed(e));
                                    panel8.add(filterSupplementaryAlignmentsCB);
                                }
                                jPanel12.add(panel8);

                                //======== panel31b ========
                                {
                                    panel31b.setLayout(new FlowLayout(FlowLayout.LEFT));

                                    //---- jLabel11b ----
                                    jLabel11b.setText("Hidden SAM tags:");
                                    jLabel11b.setPreferredSize(new Dimension(120, 16));
                                    panel31b.add(jLabel11b);

                                    //---- samHiddenTagsField ----
                                    samHiddenTagsField.setText("jTextField1b");
                                    samHiddenTagsField.setPreferredSize(new Dimension(250, 28));
                                    samHiddenTagsField.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            samHiddenTagsFieldFocusLost(e);
                                        }
                                    });
                                    samHiddenTagsField.addActionListener(e -> samHiddenTagsFieldActionPerformed(e));
                                    panel31b.add(samHiddenTagsField);
                                }
                                jPanel12.add(panel31b);
                            }
                            alignmentPanel.add(jPanel12);

                            //---- vSpacer5 ----
                            vSpacer5.setPreferredSize(new Dimension(10, 5));
                            alignmentPanel.add(vSpacer5);

                            //======== panel34 ========
                            {
                                panel34.setBorder(new TitledBorder("Coverage Track Options"));
                                panel34.setLayout(new FlowLayout(FlowLayout.LEFT));

                                //======== panel5 ========
                                {
                                    panel5.setLayout(new FlowLayout(FlowLayout.LEFT));

                                    //---- jLabel26 ----
                                    jLabel26.setText("Coverage allele-fraction threshold:");
                                    panel5.add(jLabel26);

                                    //---- snpThresholdField ----
                                    snpThresholdField.setText("0");
                                    snpThresholdField.setPreferredSize(new Dimension(60, 28));
                                    snpThresholdField.addActionListener(e -> snpThresholdFieldActionPerformed(e));
                                    snpThresholdField.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            snpThresholdFieldFocusLost(e);
                                        }
                                    });
                                    panel5.add(snpThresholdField);

                                    //---- hSpacer2 ----
                                    hSpacer2.setPreferredSize(new Dimension(50, 10));
                                    panel5.add(hSpacer2);
                                }
                                panel34.add(panel5);

                                //---- useAlleleQualityCB ----
                                useAlleleQualityCB.setText("Quality weight allele fraction");
                                useAlleleQualityCB.addActionListener(e -> useAlleleQualityCBActionPerformed(e));
                                panel34.add(useAlleleQualityCB);
                            }
                            alignmentPanel.add(panel34);

                            //======== panel3 ========
                            {
                                panel3.setBorder(new TitledBorder("Splice Junction Track Options"));
                                panel3.setLayout(new FlowLayout(FlowLayout.LEFT));

                                //---- showJunctionFlankingRegionsCB ----
                                showJunctionFlankingRegionsCB.setText("Show flanking regions");
                                showJunctionFlankingRegionsCB.addActionListener(e -> showJunctionFlankingRegionsCBActionPerformed(e));
                                panel3.add(showJunctionFlankingRegionsCB);

                                //---- label15 ----
                                label15.setText("Min flanking width:");
                                panel3.add(label15);

                                //---- junctionFlankingTextField ----
                                junctionFlankingTextField.setPreferredSize(new Dimension(80, 28));
                                junctionFlankingTextField.addActionListener(e -> junctionFlankingTextFieldActionPerformed(e));
                                junctionFlankingTextField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        junctionFlankingTextFieldFocusLost(e);
                                    }
                                });
                                panel3.add(junctionFlankingTextField);

                                //---- label16 ----
                                label16.setText("Min junction coverage:");
                                panel3.add(label16);

                                //---- junctionCoverageTextField ----
                                junctionCoverageTextField.setPreferredSize(new Dimension(80, 28));
                                junctionCoverageTextField.addActionListener(e -> junctionCoverageTextFieldActionPerformed(e));
                                junctionCoverageTextField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        junctionCoverageTextFieldFocusLost(e);
                                    }
                                });
                                panel3.add(junctionCoverageTextField);
                            }
                            alignmentPanel.add(panel3);

                            //---- vSpacer6 ----
                            vSpacer6.setPreferredSize(new Dimension(10, 5));
                            alignmentPanel.add(vSpacer6);

                            //======== panel2 ========
                            {
                                panel2.setBorder(new TitledBorder("Insert Size Options"));
                                panel2.setToolTipText("These options control the color coding of paired alignments by inferred insert size.  Base pair values set default values.  If \"compute\" is selected, values are computed from the actual size distribution of each library.");
                                panel2.setLayout(new GridLayout());

                                //======== panel19 ========
                                {
                                    panel19.setLayout(new GridLayout());

                                    //======== panel16 ========
                                    {
                                        panel16.setLayout(new GridBagLayout());
                                        ((GridBagLayout)panel16.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
                                        ((GridBagLayout)panel16.getLayout()).rowHeights = new int[] {0, 0, 0};
                                        ((GridBagLayout)panel16.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
                                        ((GridBagLayout)panel16.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                                        //---- label9 ----
                                        label9.setText("Defaults ");
                                        panel16.add(label9, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 5, 5), 0, 0));

                                        //---- jLabel20 ----
                                        jLabel20.setText("Minimum (bp):");
                                        panel16.add(jLabel20, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 5, 5), 0, 0));

                                        //---- insertSizeMinThresholdField ----
                                        insertSizeMinThresholdField.setText("0");
                                        insertSizeMinThresholdField.setPreferredSize(new Dimension(60, 28));
                                        insertSizeMinThresholdField.setMinimumSize(new Dimension(60, 28));
                                        insertSizeMinThresholdField.addActionListener(e -> {
			insertSizeThresholdFieldActionPerformed(e);
			insertSizeMinThresholdFieldActionPerformed(e);
			insertSizeMinThresholdFieldActionPerformed(e);
			insertSizeMinThresholdFieldActionPerformed(e);
		});
                                        insertSizeMinThresholdField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                insertSizeThresholdFieldFocusLost(e);
                                                insertSizeMinThresholdFieldFocusLost(e);
                                            }
                                        });
                                        panel16.add(insertSizeMinThresholdField, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 5, 0), 0, 0));

                                        //---- jLabel17 ----
                                        jLabel17.setText("Maximum (bp):");
                                        panel16.add(jLabel17, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 5), 0, 0));

                                        //---- insertSizeThresholdField ----
                                        insertSizeThresholdField.setText("0");
                                        insertSizeThresholdField.setPreferredSize(new Dimension(60, 28));
                                        insertSizeThresholdField.addActionListener(e -> {
			insertSizeThresholdFieldActionPerformed(e);
			insertSizeThresholdFieldActionPerformed(e);
		});
                                        insertSizeThresholdField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                insertSizeThresholdFieldFocusLost(e);
                                            }
                                        });
                                        panel16.add(insertSizeThresholdField, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));
                                    }
                                    panel19.add(panel16);

                                    //======== panel15 ========
                                    {
                                        panel15.setLayout(new GridBagLayout());
                                        ((GridBagLayout)panel15.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
                                        ((GridBagLayout)panel15.getLayout()).rowHeights = new int[] {0, 0, 0};
                                        ((GridBagLayout)panel15.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
                                        ((GridBagLayout)panel15.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                                        //---- isizeComputeCB ----
                                        isizeComputeCB.setText("Compute");
                                        isizeComputeCB.setToolTipText("Min and max values are computed from the actual size distribution of each library.");
                                        isizeComputeCB.addActionListener(e -> {
			isizeComputeCBActionPerformed(e);
			isizeComputeCBActionPerformed(e);
			isizeComputeCBActionPerformed(e);
		});
                                        panel15.add(isizeComputeCB, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 5, 5), 0, 0));

                                        //---- jLabel30 ----
                                        jLabel30.setText("Minimum (percentile):");
                                        panel15.add(jLabel30, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 5, 5), 0, 0));

                                        //---- insertSizeMinPercentileField ----
                                        insertSizeMinPercentileField.setText("0");
                                        insertSizeMinPercentileField.setPreferredSize(new Dimension(60, 28));
                                        insertSizeMinPercentileField.setMinimumSize(new Dimension(60, 28));
                                        insertSizeMinPercentileField.addActionListener(e -> {
			insertSizeThresholdFieldActionPerformed(e);
			insertSizeMinThresholdFieldActionPerformed(e);
			insertSizeMinThresholdFieldActionPerformed(e);
			insertSizeMinThresholdFieldActionPerformed(e);
			insertSizeMinPercentileFieldActionPerformed(e);
		});
                                        insertSizeMinPercentileField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                insertSizeThresholdFieldFocusLost(e);
                                                insertSizeMinThresholdFieldFocusLost(e);
                                                insertSizeMinPercentileFieldFocusLost(e);
                                            }
                                        });
                                        panel15.add(insertSizeMinPercentileField, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 5, 0), 0, 0));

                                        //---- jLabel18 ----
                                        jLabel18.setText("Maximum (percentile):");
                                        panel15.add(jLabel18, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 5), 0, 0));

                                        //---- insertSizeMaxPercentileField ----
                                        insertSizeMaxPercentileField.setText("0");
                                        insertSizeMaxPercentileField.setPreferredSize(new Dimension(60, 28));
                                        insertSizeMaxPercentileField.addActionListener(e -> {
			insertSizeThresholdFieldActionPerformed(e);
			insertSizeThresholdFieldActionPerformed(e);
			insertSizeMaxPercentileFieldActionPerformed(e);
		});
                                        insertSizeMaxPercentileField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                insertSizeThresholdFieldFocusLost(e);
                                                insertSizeMaxPercentileFieldFocusLost(e);
                                            }
                                        });
                                        panel15.add(insertSizeMaxPercentileField, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));
                                    }
                                    panel19.add(panel15);
                                }
                                panel2.add(panel19);
                            }
                            alignmentPanel.add(panel2);
                        }
                        panel20.setViewportView(alignmentPanel);
                    }
                    tabbedPane.addTab("Alignments", panel20);

                    //======== panel26 ========
                    {

                        //======== expressionPane ========
                        {
                            expressionPane.setLayout(null);

                            //======== jPanel8 ========
                            {
                                jPanel8.setLayout(null);

                                //======== panel18 ========
                                {
                                    panel18.setLayout(new GridBagLayout());
                                    ((GridBagLayout)panel18.getLayout()).columnWidths = new int[] {0, 0};
                                    ((GridBagLayout)panel18.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0};
                                    ((GridBagLayout)panel18.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};
                                    ((GridBagLayout)panel18.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                    //---- jLabel24 ----
                                    jLabel24.setText("Expression probe mapping options: ");
                                    panel18.add(jLabel24, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 15, 0), 0, 0));

                                    //---- jLabel21 ----
                                    jLabel21.setText("<html><i>Note: Changes will not affect currently loaded datasets.");
                                    panel18.add(jLabel21, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 15, 0), 0, 0));

                                    //---- expMapToLociCB ----
                                    expMapToLociCB.setText("<html>Map probes to target loci");
                                    expMapToLociCB.addActionListener(e -> expMapToLociCBActionPerformed(e));
                                    panel18.add(expMapToLociCB, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 15, 0), 0, 0));

                                    //---- expMapToGeneCB ----
                                    expMapToGeneCB.setText("Map probes to genes");
                                    expMapToGeneCB.addActionListener(e -> {
			expMapToGeneCBActionPerformed(e);
			expMapToGeneCBActionPerformed(e);
		});
                                    panel18.add(expMapToGeneCB, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                        new Insets(0, 0, 0, 0), 0, 0));
                                }
                                jPanel8.add(panel18);
                                panel18.setBounds(new Rectangle(new Point(20, 20), panel18.getPreferredSize()));

                                //======== panel17 ========
                                {
                                    panel17.setLayout(new GridLayout(3, 1));

                                    //---- useProbeMappingCB ----
                                    useProbeMappingCB.setText("Use probe mapping file");
                                    useProbeMappingCB.addActionListener(e -> useProbeMappingCBActionPerformed(e));
                                    panel17.add(useProbeMappingCB);

                                    //---- label22 ----
                                    label22.setText("<html><i>File path or URL to BED file containing genomic locations of probes:");
                                    panel17.add(label22);

                                    //======== panel14 ========
                                    {
                                        panel14.setLayout(null);

                                        //---- probeMappingFileTextField ----
                                        probeMappingFileTextField.addFocusListener(new FocusAdapter() {
                                            @Override
                                            public void focusLost(FocusEvent e) {
                                                probeMappingFileTextFieldFocusLost(e);
                                            }
                                        });
                                        probeMappingFileTextField.addActionListener(e -> probeMappingFileTextFieldActionPerformed(e));
                                        panel14.add(probeMappingFileTextField);
                                        probeMappingFileTextField.setBounds(0, 0, 581, probeMappingFileTextField.getPreferredSize().height);

                                        //---- probeMappingBrowseButton ----
                                        probeMappingBrowseButton.setText("Browse");
                                        probeMappingBrowseButton.addActionListener(e -> probeMappingBrowseButtonActionPerformed(e));
                                        panel14.add(probeMappingBrowseButton);
                                        probeMappingBrowseButton.setBounds(new Rectangle(new Point(592, 0), probeMappingBrowseButton.getPreferredSize()));

                                        { // compute preferred size
                                            Dimension preferredSize = new Dimension();
                                            for(int i = 0; i < panel14.getComponentCount(); i++) {
                                                Rectangle bounds = panel14.getComponent(i).getBounds();
                                                preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                                preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                            }
                                            Insets insets = panel14.getInsets();
                                            preferredSize.width += insets.right;
                                            preferredSize.height += insets.bottom;
                                            panel14.setMinimumSize(preferredSize);
                                            panel14.setPreferredSize(preferredSize);
                                        }
                                    }
                                    panel17.add(panel14);
                                }
                                jPanel8.add(panel17);
                                panel17.setBounds(20, 247, 735, panel17.getPreferredSize().height);

                                { // compute preferred size
                                    Dimension preferredSize = new Dimension();
                                    for(int i = 0; i < jPanel8.getComponentCount(); i++) {
                                        Rectangle bounds = jPanel8.getComponent(i).getBounds();
                                        preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                        preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                    }
                                    Insets insets = jPanel8.getInsets();
                                    preferredSize.width += insets.right;
                                    preferredSize.height += insets.bottom;
                                    jPanel8.setMinimumSize(preferredSize);
                                    jPanel8.setPreferredSize(preferredSize);
                                }
                            }
                            expressionPane.add(jPanel8);
                            jPanel8.setBounds(10, 30, 755, 470);

                            { // compute preferred size
                                Dimension preferredSize = new Dimension();
                                for(int i = 0; i < expressionPane.getComponentCount(); i++) {
                                    Rectangle bounds = expressionPane.getComponent(i).getBounds();
                                    preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                    preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                }
                                Insets insets = expressionPane.getInsets();
                                preferredSize.width += insets.right;
                                preferredSize.height += insets.bottom;
                                expressionPane.setMinimumSize(preferredSize);
                                expressionPane.setPreferredSize(preferredSize);
                            }
                        }
                        panel26.setViewportView(expressionPane);
                    }
                    tabbedPane.addTab("Probes", panel26);

                    //======== panel27 ========
                    {

                        //======== proxyPanel ========
                        {
                            proxyPanel.setLayout(new BoxLayout(proxyPanel, BoxLayout.X_AXIS));

                            //======== jPanel15 ========
                            {
                                jPanel15.setLayout(null);

                                //---- label3 ----
                                label3.setText("<html>Note:  do not use these settings unless you receive error or warning messages about server connections.  On most systems the correct settings will be automatically copied from your web browser.");
                                jPanel15.add(label3);
                                label3.setBounds(22, 20, 630, 63);

                                //---- clearProxySettingsButton ----
                                clearProxySettingsButton.setText("Clear All");
                                clearProxySettingsButton.addActionListener(e -> clearProxySettingsButtonActionPerformed(e));
                                jPanel15.add(clearProxySettingsButton);
                                clearProxySettingsButton.setBounds(new Rectangle(new Point(15, 620), clearProxySettingsButton.getPreferredSize()));

                                //---- proxyUsernameField ----
                                proxyUsernameField.setText("jTextField1");
                                proxyUsernameField.setEnabled(false);
                                proxyUsernameField.setPreferredSize(new Dimension(500, 28));
                                proxyUsernameField.addActionListener(e -> proxyUsernameFieldActionPerformed(e));
                                proxyUsernameField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        proxyUsernameFieldFocusLost(e);
                                    }
                                });
                                jPanel15.add(proxyUsernameField);
                                proxyUsernameField.setBounds(120, 495, 615, proxyUsernameField.getPreferredSize().height);

                                //---- jLabel28 ----
                                jLabel28.setText("Username:");
                                jPanel15.add(jLabel28);
                                jLabel28.setBounds(20, 495, jLabel28.getPreferredSize().width, 28);

                                //---- authenticateProxyCB ----
                                authenticateProxyCB.setText("Authentication required");
                                authenticateProxyCB.addActionListener(e -> authenticateProxyCBActionPerformed(e));
                                jPanel15.add(authenticateProxyCB);
                                authenticateProxyCB.setBounds(20, 455, 571, authenticateProxyCB.getPreferredSize().height);

                                //---- jLabel29 ----
                                jLabel29.setText("Password:");
                                jPanel15.add(jLabel29);
                                jLabel29.setBounds(20, 540, 66, 28);

                                //---- proxyPasswordField ----
                                proxyPasswordField.setText("jPasswordField1");
                                proxyPasswordField.setEnabled(false);
                                proxyPasswordField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        proxyPasswordFieldFocusLost(e);
                                    }
                                });
                                jPanel15.add(proxyPasswordField);
                                proxyPasswordField.setBounds(120, 540, 615, proxyPasswordField.getPreferredSize().height);

                                //---- proxyHostField ----
                                proxyHostField.setText("jTextField1");
                                proxyHostField.setPreferredSize(new Dimension(500, 28));
                                proxyHostField.addActionListener(e -> proxyHostFieldActionPerformed(e));
                                proxyHostField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        proxyHostFieldFocusLost(e);
                                    }
                                });
                                jPanel15.add(proxyHostField);
                                proxyHostField.setBounds(120, 155, 615, proxyHostField.getPreferredSize().height);

                                //---- proxyPortField ----
                                proxyPortField.setText("jTextField1");
                                proxyPortField.addActionListener(e -> proxyPortFieldActionPerformed(e));
                                proxyPortField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        proxyPortFieldFocusLost(e);
                                    }
                                });
                                jPanel15.add(proxyPortField);
                                proxyPortField.setBounds(120, 213, 615, proxyPortField.getPreferredSize().height);

                                //---- jLabel27 ----
                                jLabel27.setText("Proxy port:");
                                jPanel15.add(jLabel27);
                                jLabel27.setBounds(20, 213, 95, 28);

                                //---- jLabel23 ----
                                jLabel23.setText("Proxy host:");
                                jPanel15.add(jLabel23);
                                jLabel23.setBounds(20, 155, 95, 28);

                                //---- useProxyCB ----
                                useProxyCB.setText("Use proxy");
                                useProxyCB.addActionListener(e -> useProxyCBActionPerformed(e));
                                jPanel15.add(useProxyCB);
                                useProxyCB.setBounds(new Rectangle(new Point(20, 105), useProxyCB.getPreferredSize()));

                                //---- proxyTypeCB ----
                                proxyTypeCB.setModel(new DefaultComboBoxModel<>(new String[] {
                                    "HTTP",
                                    "SOCKS",
                                    "DIRECT"
                                }));
                                proxyTypeCB.addActionListener(e -> proxyTypeCBActionPerformed(e));
                                jPanel15.add(proxyTypeCB);
                                proxyTypeCB.setBounds(120, 271, 615, proxyTypeCB.getPreferredSize().height);

                                //---- label27 ----
                                label27.setText("Proxy type:");
                                jPanel15.add(label27);
                                label27.setBounds(20, 271, 95, 27);

                                //---- label35 ----
                                label35.setText("<html>Whitelist:  <i>comma delimited list of hosts to whitelist (bypass proxy)</i>");
                                jPanel15.add(label35);
                                label35.setBounds(new Rectangle(new Point(20, 328), label35.getPreferredSize()));

                                //---- proxyWhitelistTextArea ----
                                proxyWhitelistTextArea.addActionListener(e -> proxyWhitelistTextAreaActionPerformed(e));
                                proxyWhitelistTextArea.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        proxyWhitelistTextAreaFocusLost(e);
                                    }
                                });
                                jPanel15.add(proxyWhitelistTextArea);
                                proxyWhitelistTextArea.setBounds(20, 355, 715, 35);

                                { // compute preferred size
                                    Dimension preferredSize = new Dimension();
                                    for(int i = 0; i < jPanel15.getComponentCount(); i++) {
                                        Rectangle bounds = jPanel15.getComponent(i).getBounds();
                                        preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                        preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                    }
                                    Insets insets = jPanel15.getInsets();
                                    preferredSize.width += insets.right;
                                    preferredSize.height += insets.bottom;
                                    jPanel15.setMinimumSize(preferredSize);
                                    jPanel15.setPreferredSize(preferredSize);
                                }
                            }
                            proxyPanel.add(jPanel15);
                        }
                        panel27.setViewportView(proxyPanel);
                    }
                    tabbedPane.addTab("Proxy", panel27);

                    //======== panel30 ========
                    {

                        //======== dbPanel ========
                        {
                            dbPanel.setLayout(null);

                            //---- label20 ----
                            label20.setText("<html><b>Database configuration  <i>(experimental, subject to change)");
                            label20.setFont(new Font("Lucida Grande", Font.PLAIN, 14));
                            dbPanel.add(label20);
                            label20.setBounds(new Rectangle(new Point(50, 20), label20.getPreferredSize()));

                            //======== panel21 ========
                            {
                                panel21.setLayout(new GridBagLayout());
                                ((GridBagLayout)panel21.getLayout()).columnWidths = new int[] {0, 0, 0};
                                ((GridBagLayout)panel21.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                ((GridBagLayout)panel21.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                                ((GridBagLayout)panel21.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                //---- label17 ----
                                label17.setText("Host:");
                                panel21.add(label17, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 5, 5), 0, 0));

                                //---- label19 ----
                                label19.setText("Name:");
                                panel21.add(label19, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 5, 5), 0, 0));

                                //---- dbNameField ----
                                dbNameField.setPreferredSize(new Dimension(500, 28));
                                dbNameField.addActionListener(e -> dbNameFieldActionPerformed(e));
                                dbNameField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        dbNameFieldFocusLost(e);
                                    }
                                });
                                panel21.add(dbNameField, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 5, 0), 0, 0));

                                //---- dbHostField ----
                                dbHostField.setPreferredSize(new Dimension(500, 28));
                                dbHostField.addActionListener(e -> dbHostFieldActionPerformed(e));
                                dbHostField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        dbHostFieldFocusLost(e);
                                    }
                                });
                                panel21.add(dbHostField, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 5, 0), 0, 0));

                                //---- label18 ----
                                label18.setText("Port:");
                                panel21.add(label18, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 0, 5), 0, 0));

                                //---- dbPortField ----
                                dbPortField.setPreferredSize(new Dimension(500, 28));
                                dbPortField.addActionListener(e -> dbPortFieldActionPerformed(e));
                                dbPortField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        dbPortFieldFocusLost(e);
                                    }
                                });
                                panel21.add(dbPortField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 0, 0), 0, 0));
                            }
                            dbPanel.add(panel21);
                            panel21.setBounds(new Rectangle(new Point(20, 76), panel21.getPreferredSize()));

                            { // compute preferred size
                                Dimension preferredSize = new Dimension();
                                for(int i = 0; i < dbPanel.getComponentCount(); i++) {
                                    Rectangle bounds = dbPanel.getComponent(i).getBounds();
                                    preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                    preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                }
                                Insets insets = dbPanel.getInsets();
                                preferredSize.width += insets.right;
                                preferredSize.height += insets.bottom;
                                dbPanel.setMinimumSize(preferredSize);
                                dbPanel.setPreferredSize(preferredSize);
                            }
                        }
                        panel30.setViewportView(dbPanel);
                    }
                    tabbedPane.addTab("Database", panel30);

                    //======== panel29 ========
                    {

                        //======== advancedPanel ========
                        {
                            advancedPanel.setBorder(new EmptyBorder(1, 10, 1, 10));
                            advancedPanel.setLayout(new GridBagLayout());
                            ((GridBagLayout)advancedPanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                            ((GridBagLayout)advancedPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                            ((GridBagLayout)advancedPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
                            ((GridBagLayout)advancedPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                            //---- clearGenomeCacheButton ----
                            clearGenomeCacheButton.setText("Clear Genome Cache");
                            clearGenomeCacheButton.addActionListener(e -> clearGenomeCacheButtonActionPerformed(e));
                            advancedPanel.add(clearGenomeCacheButton, new GridBagConstraints(7, 2, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- enablePortCB ----
                            enablePortCB.setText("Enable port");
                            enablePortCB.addActionListener(e -> enablePortCBActionPerformed(e));
                            advancedPanel.add(enablePortCB, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- portField ----
                            portField.setText("60151");
                            portField.addActionListener(e -> portFieldActionPerformed(e));
                            portField.addFocusListener(new FocusAdapter() {
                                @Override
                                public void focusLost(FocusEvent e) {
                                    portFieldFocusLost(e);
                                }
                            });
                            advancedPanel.add(portField, new GridBagConstraints(4, 0, 2, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- jLabel22 ----
                            jLabel22.setFont(new Font("Lucida Grande", Font.ITALIC, 13));
                            jLabel22.setText("Enable port to send commands and http requests to IGV. ");
                            advancedPanel.add(jLabel22, new GridBagConstraints(6, 0, 3, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 0), 0, 0));

                            //---- vSpacer12 ----
                            vSpacer12.setPreferredSize(new Dimension(10, 40));
                            advancedPanel.add(vSpacer12, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- genomeUpdateCB ----
                            genomeUpdateCB.setText("<html>Automatically check for updated genomes.    &nbsp;&nbsp;&nbsp;   <i>Most users should leave this checked.");
                            genomeUpdateCB.addActionListener(e -> genomeUpdateCBActionPerformed(e));
                            advancedPanel.add(genomeUpdateCB, new GridBagConstraints(0, 6, 8, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- jLabel6 ----
                            jLabel6.setText("Data Registry URL");
                            advancedPanel.add(jLabel6, new GridBagConstraints(2, 4, 3, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- dataServerURLTextField ----
                            dataServerURLTextField.setEnabled(false);
                            dataServerURLTextField.addActionListener(e -> dataServerURLTextFieldActionPerformed(e));
                            dataServerURLTextField.addFocusListener(new FocusAdapter() {
                                @Override
                                public void focusLost(FocusEvent e) {
                                    dataServerURLTextFieldFocusLost(e);
                                }
                            });
                            advancedPanel.add(dataServerURLTextField, new GridBagConstraints(5, 4, 4, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 0), 0, 0));

                            //---- jLabel1 ----
                            jLabel1.setText("Genome Server URL");
                            advancedPanel.add(jLabel1, new GridBagConstraints(2, 3, 3, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- genomeServerURLTextField ----
                            genomeServerURLTextField.setText("jTextField1");
                            genomeServerURLTextField.setEnabled(false);
                            genomeServerURLTextField.addActionListener(e -> genomeServerURLTextFieldActionPerformed(e));
                            genomeServerURLTextField.addFocusListener(new FocusAdapter() {
                                @Override
                                public void focusLost(FocusEvent e) {
                                    genomeServerURLTextFieldFocusLost(e);
                                }
                            });
                            advancedPanel.add(genomeServerURLTextField, new GridBagConstraints(5, 3, 4, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 0), 0, 0));

                            //---- editServerPropertiesCB ----
                            editServerPropertiesCB.setText("Edit server properties");
                            editServerPropertiesCB.addActionListener(e -> editServerPropertiesCBActionPerformed(e));
                            advancedPanel.add(editServerPropertiesCB, new GridBagConstraints(0, 2, 5, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- jButton1 ----
                            jButton1.setText("Reset to Defaults");
                            jButton1.addActionListener(e -> jButton1ActionPerformed(e));
                            advancedPanel.add(jButton1, new GridBagConstraints(5, 2, 2, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- vSpacer11 ----
                            vSpacer11.setPreferredSize(new Dimension(10, 40));
                            advancedPanel.add(vSpacer11, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- autoFileDisoveryCB ----
                            autoFileDisoveryCB.setText("Automatically discover index and coverage files.");
                            autoFileDisoveryCB.addActionListener(e -> autoFileDisoveryCBActionPerformed(e));
                            advancedPanel.add(autoFileDisoveryCB, new GridBagConstraints(0, 7, 8, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- igvDirectoryButton ----
                            igvDirectoryButton.setText("Move...");
                            igvDirectoryButton.addActionListener(e -> igvDirectoryButtonActionPerformed(e));
                            advancedPanel.add(igvDirectoryButton, new GridBagConstraints(8, 15, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 0, 0), 0, 0));

                            //---- igvDirectoryField ----
                            igvDirectoryField.setBorder(new BevelBorder(BevelBorder.LOWERED));
                            advancedPanel.add(igvDirectoryField, new GridBagConstraints(2, 15, 6, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 0, 5), 0, 0));

                            //---- label21 ----
                            label21.setText("IGV Directory: ");
                            advancedPanel.add(label21, new GridBagConstraints(0, 14, 4, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //======== tooltipOptionsPanel ========
                            {
                                tooltipOptionsPanel.setLayout(null);

                                //---- label24 ----
                                label24.setText("Tooltip initial delay (ms)");
                                tooltipOptionsPanel.add(label24);
                                label24.setBounds(0, 11, 185, label24.getPreferredSize().height);

                                //---- label25 ----
                                label25.setText("Tooltip reshow delay (ms)");
                                tooltipOptionsPanel.add(label25);
                                label25.setBounds(0, 38, 185, 23);

                                //---- label26 ----
                                label26.setText("Tooltip dismiss delay (ms)");
                                tooltipOptionsPanel.add(label26);
                                label26.setBounds(0, 70, 185, 16);

                                //---- toolTipInitialDelayField ----
                                toolTipInitialDelayField.addActionListener(e -> toolTipInitialDelayFieldActionPerformed(e));
                                toolTipInitialDelayField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        toolTipInitialDelayFieldFocusLost(e);
                                    }
                                });
                                tooltipOptionsPanel.add(toolTipInitialDelayField);
                                toolTipInitialDelayField.setBounds(220, 5, 455, toolTipInitialDelayField.getPreferredSize().height);

                                //---- tooltipReshowDelayField ----
                                tooltipReshowDelayField.addActionListener(e -> tooltipReshowDelayFieldActionPerformed(e));
                                tooltipReshowDelayField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        tooltipReshowDelayFieldFocusLost(e);
                                    }
                                });
                                tooltipOptionsPanel.add(tooltipReshowDelayField);
                                tooltipReshowDelayField.setBounds(220, 35, 455, 28);

                                //---- tooltipDismissDelayField ----
                                tooltipDismissDelayField.addActionListener(e -> tooltipDismissDelayFieldActionPerformed(e));
                                tooltipDismissDelayField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        tooltipDismissDelayFieldFocusLost(e);
                                    }
                                });
                                tooltipOptionsPanel.add(tooltipDismissDelayField);
                                tooltipDismissDelayField.setBounds(220, 64, 455, 28);

                                { // compute preferred size
                                    Dimension preferredSize = new Dimension();
                                    for(int i = 0; i < tooltipOptionsPanel.getComponentCount(); i++) {
                                        Rectangle bounds = tooltipOptionsPanel.getComponent(i).getBounds();
                                        preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                        preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                    }
                                    Insets insets = tooltipOptionsPanel.getInsets();
                                    preferredSize.width += insets.right;
                                    preferredSize.height += insets.bottom;
                                    tooltipOptionsPanel.setMinimumSize(preferredSize);
                                    tooltipOptionsPanel.setPreferredSize(preferredSize);
                                }
                            }
                            advancedPanel.add(tooltipOptionsPanel, new GridBagConstraints(0, 10, 9, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 0), 0, 0));

                            //---- antialiasingCB ----
                            antialiasingCB.setText("Enable antialiasing");
                            antialiasingCB.addActionListener(e -> antialiasingCBActionPerformed(e));
                            advancedPanel.add(antialiasingCB, new GridBagConstraints(0, 8, 5, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- label5 ----
                            label5.setText("BLAT URL");
                            advancedPanel.add(label5, new GridBagConstraints(1, 12, 2, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- blatURLField ----
                            blatURLField.addFocusListener(new FocusAdapter() {
                                @Override
                                public void focusLost(FocusEvent e) {
                                    blatURLFieldFocusLost(e);
                                }
                            });
                            blatURLField.addActionListener(e -> blatURLFieldActionPerformed(e));
                            advancedPanel.add(blatURLField, new GridBagConstraints(3, 12, 6, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 0), 0, 0));

                            //---- vSpacer8 ----
                            vSpacer8.setPreferredSize(new Dimension(10, 40));
                            advancedPanel.add(vSpacer8, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- vSpacer9 ----
                            vSpacer9.setPreferredSize(new Dimension(10, 40));
                            advancedPanel.add(vSpacer9, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));

                            //---- vSpacer10 ----
                            vSpacer10.setPreferredSize(new Dimension(10, 40));
                            advancedPanel.add(vSpacer10, new GridBagConstraints(1, 13, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 5, 5), 0, 0));
                        }
                        panel29.setViewportView(advancedPanel);
                    }
                    tabbedPane.addTab("Advanced", panel29);

                    //======== panel36 ========
                    {

                        //======== cramPanel ========
                        {
                            cramPanel.setBorder(new EmptyBorder(1, 10, 1, 10));
                            cramPanel.setLayout(null);

                            //======== panel28 ========
                            {
                                panel28.setLayout(new FlowLayout(FlowLayout.LEFT));
                            }
                            cramPanel.add(panel28);
                            panel28.setBounds(new Rectangle(new Point(15, 7), panel28.getPreferredSize()));

                            //======== panel37 ========
                            {
                                panel37.setLayout(new FlowLayout(FlowLayout.LEFT));

                                //---- label28 ----
                                label28.setText("Reference cache size (mb): ");
                                panel37.add(label28);

                                //---- cramCacheSizeField ----
                                cramCacheSizeField.setPreferredSize(new Dimension(100, 26));
                                cramCacheSizeField.addActionListener(e -> cramCacheSizeFieldActionPerformed(e));
                                cramCacheSizeField.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        cramCacheSizeFieldFocusLost(e);
                                    }
                                });
                                panel37.add(cramCacheSizeField);
                            }
                            cramPanel.add(panel37);
                            panel37.setBounds(new Rectangle(new Point(20, 93), panel37.getPreferredSize()));

                            //======== panel38 ========
                            {
                                panel38.setLayout(new FlowLayout(FlowLayout.LEFT));

                                //---- label29 ----
                                label29.setText("Cache directory: ");
                                panel38.add(label29);

                                //---- cramCacheDirectoryField ----
                                cramCacheDirectoryField.setMinimumSize(new Dimension(400, 26));
                                cramCacheDirectoryField.setPreferredSize(new Dimension(400, 26));
                                panel38.add(cramCacheDirectoryField);

                                //---- cramCacheDirectoryButton ----
                                cramCacheDirectoryButton.setText("Change...");
                                cramCacheDirectoryButton.addActionListener(e -> cramCacheDirectoryButtonActionPerformed(e));
                                panel38.add(cramCacheDirectoryButton);
                            }
                            cramPanel.add(panel38);
                            panel38.setBounds(new Rectangle(new Point(20, 144), panel38.getPreferredSize()));

                            //---- cramCacheReferenceCB ----
                            cramCacheReferenceCB.setText("Cache reference sequences");
                            cramCacheReferenceCB.addActionListener(e -> cramCacheReferenceCBActionPerformed(e));
                            cramPanel.add(cramCacheReferenceCB);
                            cramCacheReferenceCB.setBounds(new Rectangle(new Point(20, 55), cramCacheReferenceCB.getPreferredSize()));

                            { // compute preferred size
                                Dimension preferredSize = new Dimension();
                                for(int i = 0; i < cramPanel.getComponentCount(); i++) {
                                    Rectangle bounds = cramPanel.getComponent(i).getBounds();
                                    preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                                    preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                                }
                                Insets insets = cramPanel.getInsets();
                                preferredSize.width += insets.right;
                                preferredSize.height += insets.bottom;
                                cramPanel.setMinimumSize(preferredSize);
                                cramPanel.setPreferredSize(preferredSize);
                            }
                        }
                        panel36.setViewportView(cramPanel);
                    }
                    tabbedPane.addTab("Cram", panel36);
                }
                panel6.add(tabbedPane, BorderLayout.NORTH);

                //======== okCancelButtonPanel ========
                {
                    okCancelButtonPanel.setPreferredSize(new Dimension(178, 29));
                    okCancelButtonPanel.setGroupGap(0);

                    //---- okButton ----
                    okButton.setText("OK");
                    okButton.addActionListener(e -> okButtonActionPerformed(e));
                    okCancelButtonPanel.add(okButton);

                    //---- cancelButton ----
                    cancelButton.setText("Cancel");
                    cancelButton.addActionListener(e -> cancelButtonActionPerformed(e));
                    okCancelButtonPanel.add(cancelButton);
                }
                panel6.add(okCancelButtonPanel, BorderLayout.SOUTH);
            }
            panel7.setViewportView(panel6);
        }
        contentPane.add(panel7, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroup2 ----
        ButtonGroup buttonGroup2 = new ButtonGroup();
        buttonGroup2.add(alleleFreqRB);
        buttonGroup2.add(alleleFractionRB);

        //---- buttonGroup1 ----
        ButtonGroup buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(expMapToLociCB);
        buttonGroup1.add(expMapToGeneCB);
    }// </editor-fold>//GEN-END:initComponents

    private void backgroundColorPanelMouseClicked(MouseEvent e) {
        final IGVPreferences prefMgr = PreferencesManager.getPreferences();
        Color backgroundColor = UIUtilities.showColorChooserDialog("Choose background color",
                prefMgr.getAsColor(BACKGROUND_COLOR));
        if (backgroundColor != null) {
            prefMgr.put(BACKGROUND_COLOR, ColorUtilities.colorToString(backgroundColor));
            IGV.getInstance().getMainPanel().setBackground(backgroundColor);
            backgroundColorPanel.setBackground(backgroundColor);
        }

    }



    private void resetBackgroundButtonActionPerformed(ActionEvent e) {
        final IGVPreferences prefMgr = PreferencesManager.getPreferences();
        prefMgr.remove(BACKGROUND_COLOR);
        Color backgroundColor = prefMgr.getAsColor(BACKGROUND_COLOR);
        if (backgroundColor != null) {
            prefMgr.put(BACKGROUND_COLOR, ColorUtilities.colorToString(backgroundColor));
            IGV.getInstance().getMainPanel().setBackground(backgroundColor);
            backgroundColorPanel.setBackground(backgroundColor);
        }
    }

    private void resetFontButtonActionPerformed(ActionEvent e) {
        FontManager.resetDefaultFont();
        updateFontField();
    }


    private void filterSecondaryAlignmentsCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(
                SAM_FILTER_SECONDARY_ALIGNMENTS,
                String.valueOf(filterSecondaryAlignmentsCB.isSelected()));
    }


    private void filterSupplementaryAlignmentsCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(
                SAM_FILTER_SUPPLEMENTARY_ALIGNMENTS,
                String.valueOf(filterSupplementaryAlignmentsCB.isSelected()));
    }

    private void antialiasingCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(
                ENABLE_ANTIALISING,
                String.valueOf(antialiasingCB.isSelected()));

    }

    private void useAlleleQualityCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(SAM_ALLELE_USE_QUALITY, String.valueOf(
                useAlleleQualityCB.isSelected()));
    }

    private void featureVisibilityWindowFieldActionPerformed(ActionEvent e) {
        boolean valid = false;
        String vw = featureVisibilityWindowField.getText().trim();
        try {
            double val = Double.parseDouble(vw);
            valid = true;
            updatedPreferenceMap.put(DEFAULT_VISIBILITY_WINDOW, vw);
        } catch (NumberFormatException numberFormatException) {
            valid = false;
        }
        if (!valid && e != null) {
            junctionFlankingTextField.setText(prefMgr.get(DEFAULT_VISIBILITY_WINDOW));
            MessageUtils.showMessage("Visibility window must be a number");
        }
    }


    private void featureVisibilityWindowFieldFocusLost(FocusEvent e) {
        this.featureVisibilityWindowFieldActionPerformed(null);
    }

    private void fontChangeButtonActionPerformed(ActionEvent e) {
        Font defaultFont = FontManager.getDefaultFont();
        FontChooser chooser = new FontChooser(this, defaultFont);
        chooser.setModal(true);
        chooser.setVisible(true);
        if (!chooser.isCanceled()) {
            Font font = chooser.getSelectedFont();
            if (font != null) {
                prefMgr.put(DEFAULT_FONT_FAMILY, font.getFamily());
                prefMgr.put(DEFAULT_FONT_SIZE, String.valueOf(font.getSize()));
                int attrs = Font.PLAIN;
                if (font.isBold()) attrs = Font.BOLD;
                if (font.isItalic()) attrs |= Font.ITALIC;
                prefMgr.put(DEFAULT_FONT_ATTRIBUTE, String.valueOf(attrs));
                FontManager.updateDefaultFont();
                updateFontField();
                IGV.getInstance().repaint();
            }
        }
    }


    private void expMapToLociCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expMapToLociCBActionPerformed
        updatedPreferenceMap.put(PROBE_MAPPING_KEY, String.valueOf(expMapToGeneCB.isSelected()));
    }

    private void clearGenomeCacheButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearGenomeCacheButtonActionPerformed
        GenomeManager.getInstance().clearGenomeCache();
        JOptionPane.showMessageDialog(this, "<html>Cached genomes have been removed.");
    }

    private void editServerPropertiesCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editServerPropertiesCBActionPerformed
        boolean edit = editServerPropertiesCB.isSelected();
        dataServerURLTextField.setEnabled(edit);
        genomeServerURLTextField.setEnabled(edit);
    }

    private void dataServerURLTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dataServerURLTextFieldFocusLost
        String attributeName = dataServerURLTextField.getText().trim();
        updatedPreferenceMap.put(DATA_SERVER_URL_KEY, attributeName);
    }


    private void igvDirectoryButtonActionPerformed(ActionEvent e) {
        final File igvDirectory = DirectoryManager.getIgvDirectory();
        final File newDirectory = FileDialogUtils.chooseDirectory("Select IGV directory", DirectoryManager.getUserDirectory());
        if (newDirectory != null && !newDirectory.equals(igvDirectory.getParentFile())) {
            newIGVDirectory = new File(newDirectory, "igv");
            igvDirectoryField.setText(newIGVDirectory.getAbsolutePath());

        }
    }

    private void dataServerURLTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dataServerURLTextFieldActionPerformed
        String attributeName = dataServerURLTextField.getText().trim();
        updatedPreferenceMap.put(DATA_SERVER_URL_KEY, attributeName);
    }

    private void genomeServerURLTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_genomeServerURLTextFieldFocusLost
        String attributeName = genomeServerURLTextField.getText().trim();
        updatedPreferenceMap.put(GENOMES_SERVER_URL, attributeName);
    }

    private void genomeServerURLTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_genomeServerURLTextFieldActionPerformed
        String attributeName = genomeServerURLTextField.getText().trim();
        updatedPreferenceMap.put(GENOMES_SERVER_URL, attributeName);
    }


    private void blatURLFieldFocusLost(FocusEvent e) {
        String attributeName = blatURLField.getText().trim();
        updatedPreferenceMap.put(BLAT_URL, attributeName);

    }

    private void blatURLFieldActionPerformed(ActionEvent e) {
        blatURLFieldFocusLost(null);
    }

    private void normalizeCoverageCBFocusLost(FocusEvent e) {
        // TODO add your code here
    }


    private void showJunctionTrackCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showCovTrackCBActionPerformed
        final boolean junctionTrackEnabled = showJunctionTrackCB.isSelected();
        updatedPreferenceMap.put(SAM_SHOW_JUNCTION_TRACK, String.valueOf(junctionTrackEnabled));
    }

    private void showJunctionFlankingRegionsCBActionPerformed(java.awt.event.ActionEvent evt) {
        final boolean junctionFlankingRegionsEnabled = showJunctionFlankingRegionsCB.isSelected();
        updatedPreferenceMap.put(SAM_SHOW_JUNCTION_FLANKINGREGIONS,
                String.valueOf(junctionFlankingRegionsEnabled));
    }

    private void junctionFlankingTextFieldFocusLost(FocusEvent e) {
        junctionFlankingTextFieldActionPerformed(null);
    }

    private void junctionFlankingTextFieldActionPerformed(ActionEvent e) {
        boolean valid = false;
        String flankingWidth = junctionFlankingTextField.getText().trim();
        try {
            int val = Integer.parseInt(flankingWidth);
            if (val >= 0) {
                valid = true;
                updatedPreferenceMap.put(SAM_JUNCTION_MIN_FLANKING_WIDTH, flankingWidth);
            }

        } catch (NumberFormatException numberFormatException) {
        }
        if (!valid && e != null) {
            junctionFlankingTextField.setText(prefMgr.get(SAM_JUNCTION_MIN_FLANKING_WIDTH));
            MessageUtils.showMessage("Flanking width must be a positive integer.");
        }
    }

    private void junctionCoverageTextFieldActionPerformed(ActionEvent e) {
        junctionCoverageTextFieldFocusLost(null);
    }

    private void junctionCoverageTextFieldFocusLost(FocusEvent e) {
        boolean valid = false;
        String minCoverage = junctionCoverageTextField.getText().trim();
        try {
            int val = Integer.parseInt(minCoverage);
            if (val >= 0) {
                valid = true;
                updatedPreferenceMap.put(SAM_JUNCTION_MIN_COVERAGE, minCoverage);
            }
        } catch (NumberFormatException numberFormatException) {
            valid = false;
        }
        if (!valid && e != null) {
            junctionCoverageTextField.setText(prefMgr.get(SAM_JUNCTION_MIN_COVERAGE));
            MessageUtils.showMessage("Minimum junction coverage must be a positive integer.");

        }
    }


    private void insertSizeThresholdFieldFocusLost(java.awt.event.FocusEvent evt) {
        this.insertSizeThresholdFieldActionPerformed(null);
    }

    private void insertSizeThresholdFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String insertThreshold = insertSizeThresholdField.getText().trim();
        try {
            Integer.parseInt(insertThreshold);
            updatedPreferenceMap.put(SAM_MAX_INSERT_SIZE_THRESHOLD, insertThreshold);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("BlastMapping quality threshold must be an integer.");
        }
    }

    private void insertSizeMinThresholdFieldFocusLost(FocusEvent e) {
        insertSizeMinThresholdFieldActionPerformed(null);
    }

    private void insertSizeMinThresholdFieldActionPerformed(ActionEvent e) {
        String insertThreshold = insertSizeMinThresholdField.getText().trim();
        try {
            Integer.parseInt(insertThreshold);
            updatedPreferenceMap.put(SAM_MIN_INSERT_SIZE_THRESHOLD, insertThreshold);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("BlastMapping quality threshold must be an integer.");
        }
    }


    private void mappingQualityThresholdFieldFocusLost(java.awt.event.FocusEvent evt) {
        mappingQualityThresholdFieldActionPerformed(null);
    }//GEN-LAST:event_mappingQualityThresholdFieldFocusLost

    private void mappingQualityThresholdFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String qualityThreshold = mappingQualityThresholdField.getText().trim();
        try {
            Integer.parseInt(qualityThreshold);
            updatedPreferenceMap.put(SAM_QUALITY_THRESHOLD, qualityThreshold);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage(
                    "BlastMapping quality threshold must be an integer.");
        }
    }

    private void samFlagIndelsCBActionPerformed(ActionEvent e) {
        final boolean flagInsertions = samFlagIndelsCB.isSelected();
        updatedPreferenceMap.put(SAM_FLAG_LARGE_INDELS, String.valueOf(flagInsertions));
        samFlagIndelsThresholdField.setEnabled(flagInsertions);
    }


    private void samFlagIndelsThresholdFieldFocusLost(FocusEvent e) {
        samFlagIndelsThresholdFieldActionPerformed(null);
    }

    private void samFlagIndelsThresholdFieldActionPerformed(ActionEvent e) {
        String insertionThreshold = samFlagIndelsThresholdField.getText().trim();
        try {
            int tmp = Integer.parseInt(insertionThreshold);
            if (tmp <= 0) {
                inputValidated = false;
                MessageUtils.showMessage("Insertion threshold must be a positive integer.");
            } else {
                updatedPreferenceMap.put(SAM_LARGE_INDELS_THRESHOLD, insertionThreshold);
            }
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Insertion threshold must be a positive integer.");
        }
    }

    private void samFlagClippingCBActionPerformed(ActionEvent e) {
        final boolean flagClipping = samFlagClippingCB.isSelected();
        updatedPreferenceMap.put(SAM_FLAG_CLIPPING, String.valueOf(flagClipping));
        samFlagClippingThresholdField.setEnabled(flagClipping);
    }


    private void samFlagClippingThresholdFieldFocusLost(FocusEvent e) {
        samFlagClippingThresholdFieldActionPerformed(null);
    }

    private void samFlagClippingThresholdFieldActionPerformed(ActionEvent e) {
        String clippingThreshold = samFlagClippingThresholdField.getText().trim();
        try {
            int tmp = Integer.parseInt(clippingThreshold);
            if (tmp < 0) {
                inputValidated = false;
                MessageUtils.showMessage("Clipping threshold must be a non-negative integer.");
            } else {
                updatedPreferenceMap.put(SAM_CLIPPING_THRESHOLD, clippingThreshold);
            }
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Clipping threshold must be a non-negative integer.");
        }
    }

    private void hideIndelsBasesCBActionPerformed(ActionEvent e) {
        final boolean flagInsertions = hideIndelsBasesCB.isSelected();
        updatedPreferenceMap.put(SAM_HIDE_SMALL_INDEL, String.valueOf(flagInsertions));
        hideIndelsBasesField.setEnabled(flagInsertions);

    }

    private void hideIndelsBasesFieldFocusLost(FocusEvent e) {
        hideIndelsBasesFieldActionPerformed(null);
    }

    private void hideIndelsBasesFieldActionPerformed(ActionEvent e) {
        String threshold = hideIndelsBasesField.getText().trim();
        try {
            int tmp = Integer.parseInt(threshold);
            if (tmp <= 0) {
                inputValidated = false;
                MessageUtils.showMessage("Threshold must be a positive integer.");
            } else {
                updatedPreferenceMap.put(SAM_SMALL_INDEL_BP_THRESHOLD, threshold);
            }
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Threshold must be a positive integer.");
        }
    }


    private void downsampleReadsCBActionPerformed(ActionEvent e) {
        final boolean downsample = downsampleReadsCB.isSelected();
        updatedPreferenceMap.put(SAM_DOWNSAMPLE_READS, String.valueOf(downsample));
        samSamplingWindowField.setEnabled(downsample);
        samDownsampleCountField.setEnabled(downsample);
    }

    private void samSamplingWindowFieldFocusLost(FocusEvent e) {
        samSamplingWindowFieldActionPerformed(null);
    }

    private void samSamplingWindowFieldActionPerformed(ActionEvent e) {
        String samplingWindowString = samSamplingWindowField.getText().trim();
        try {
            int samplingWindow = Integer.parseInt(samplingWindowString);
            if (samplingWindow <= 0) {
                inputValidated = false;
                MessageUtils.showMessage("Down-sampling window must be a positive integer.");
            } else {
                updatedPreferenceMap.put(SAM_SAMPLING_WINDOW, samplingWindowString);
            }
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Down-sampling window must be a positive integer.");
        }
    }


    private void samDownsampleCountFieldFocusLost(java.awt.event.FocusEvent evt) {
        samDownsampleCountFieldActionPerformed(null);
    }

    private void samDownsampleCountFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String maxLevelString = samDownsampleCountField.getText().trim();
        try {
            int maxLevel = Integer.parseInt(maxLevelString);
            if (maxLevel <= 0) {
                inputValidated = false;
                MessageUtils.showMessage("Down-sampling read count must be a positive integer.");
            } else {
                updatedPreferenceMap.put(SAM_SAMPLING_COUNT, maxLevelString);
            }
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Down-sampling read count must be a positive integer.");
        }
    }

    private void samShadeMismatchedBaseCBActionPerformed(java.awt.event.ActionEvent evt) {
        if (samShadeMismatchedBaseCB.isSelected()) {
            updatedPreferenceMap.put(
                    SAM_SHADE_BASES,
                    ShadeBasesOption.QUALITY.toString());
            samMinBaseQualityField.setEnabled(samShadeMismatchedBaseCB.isSelected());
            samMaxBaseQualityField.setEnabled(samShadeMismatchedBaseCB.isSelected());
        } else {
            IGVPreferences prefMgr = PreferencesManager.getPreferences();
            if (ShadeBasesOption.QUALITY ==
                    CollUtils.valueOf(ShadeBasesOption.class, prefMgr.get(SAM_SHADE_BASES), ShadeBasesOption.QUALITY)) {
                updatedPreferenceMap.put(
                        SAM_SHADE_BASES,
                        ShadeBasesOption.NONE.toString());
                samMinBaseQualityField.setEnabled(false);
                samMaxBaseQualityField.setEnabled(false);
            }
        }
    }

    private void showCenterLineCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(
                SAM_SHOW_CENTER_LINE,
                String.valueOf(showCenterLineCB.isSelected()));

    }

    private void genomeUpdateCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(
                AUTO_UPDATE_GENOMES,
                String.valueOf(this.genomeUpdateCB.isSelected()));
    }


    private void samFlagUnmappedPairCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(
                SAM_FLAG_UNMAPPED_PAIR,
                String.valueOf(samFlagUnmappedPairCB.isSelected()));
    }

    private void samShowDuplicatesCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(
                SAM_SHOW_DUPLICATES,
                String.valueOf(!samFilterDuplicatesCB.isSelected()));
    }

    private void showSoftClippedCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(
                SAM_SHOW_SOFT_CLIPPED,
                String.valueOf(showSoftClippedCB.isSelected()));
    }


    private void quickConsensusModeCBActionPerformed() {
        updatedPreferenceMap.put(
                SAM_QUICK_CONSENSUS_MODE,
                String.valueOf(quickConsensusModeCB.isSelected()));
    }


    private void isizeComputeCBActionPerformed(ActionEvent e) {
        final boolean selected = isizeComputeCB.isSelected();
        updatedPreferenceMap.put(SAM_COMPUTE_ISIZES, String.valueOf(selected));
        insertSizeThresholdField.setEnabled(!selected);
        insertSizeMinThresholdField.setEnabled(!selected);
        insertSizeMinPercentileField.setEnabled(selected);
        insertSizeMaxPercentileField.setEnabled(selected);
    }

    public void selectTab(String tabname) {
        if (tabname == null) return;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getTitleAt(i).equalsIgnoreCase(tabname)) {
                tabbedPane.setSelectedIndex(i);
                return;
            }
        }
    }

    private void insertSizeMinPercentileFieldFocusLost(FocusEvent e) {
        insertSizeMinPercentileFieldActionPerformed(null);
    }

    private void insertSizeMinPercentileFieldActionPerformed(ActionEvent e) {
        String valueString = insertSizeMinPercentileField.getText().trim();
        try {
            Double.parseDouble(valueString);
            updatedPreferenceMap.put(SAM_MIN_INSERT_SIZE_PERCENTILE, valueString);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Minimum insert size percentile must be a number.");
        }
    }


    private void insertSizeMaxPercentileFieldFocusLost(FocusEvent e) {
        insertSizeMaxPercentileFieldActionPerformed(null);
    }

    private void insertSizeMaxPercentileFieldActionPerformed(ActionEvent e) {
        String valueString = insertSizeMaxPercentileField.getText().trim();
        try {
            Double.parseDouble(valueString);
            updatedPreferenceMap.put(SAM_MAX_INSERT_SIZE_PERCENTILE, valueString);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Maximum insert size percentile must be a number.");
        }
    }


    private void samMaxWindowSizeFieldFocusLost(java.awt.event.FocusEvent evt) {
        samMaxWindowSizeFieldActionPerformed(null);
    }

    private void samMaxWindowSizeFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String maxSAMWindowSize = String.valueOf(samMaxWindowSizeField.getText());
        try {
            Float.parseFloat(maxSAMWindowSize);
            updatedPreferenceMap.put(SAM_MAX_VISIBLE_RANGE, maxSAMWindowSize);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Visibility range must be a number.");
        }
    }


    private void samHiddenTagsFieldFocusLost(java.awt.event.FocusEvent evt) {
        samHiddenTagsFieldActionPerformed(null);
    }

    private void samHiddenTagsFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String samHiddenTags = String.valueOf(samHiddenTagsField.getText()), samHiddenTagsClean = "";
        for (String s : (samHiddenTags == null ? "" : samHiddenTags).split("[, ]")) {
            if (!s.equals("")) {
                samHiddenTagsClean += (samHiddenTagsClean.equals("") ? "" : ",") + s;
            }
        }
        samHiddenTagsClean += ","; // ensure non-empty string, which results in the option being unset
        updatedPreferenceMap.put(SAM_HIDDEN_TAGS, samHiddenTagsClean);
    }


    private void seqResolutionThresholdActionPerformed(ActionEvent e) {
        //samMaxWindowSizeFieldFocusLost(null);
    }

    private void seqResolutionThresholdFocusLost(FocusEvent e) {
        String seqResolutionSize = String.valueOf(seqResolutionThreshold.getText());
        try {
            float value = Float.parseFloat(seqResolutionSize.replace(",", ""));
            if (value < 1 || value > 10000) {
                MessageUtils.showMessage("Visibility range must be a number between 1 and 10000.");
            } else {
                updatedPreferenceMap.put(MAX_SEQUENCE_RESOLUTION, seqResolutionSize);
            }
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Visibility range must be a number between 1 and 10000.");
        }

    }


    private void chartDrawTrackNameCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(CHART_DRAW_TRACK_NAME,
                String.valueOf(chartDrawTrackNameCB.isSelected()));
    }

    private void autoscaleCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(CHART_AUTOSCALE, String.valueOf(autoscaleCB.isSelected()));
    }


    private void colorBordersCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(
                CHART_COLOR_BORDERS,
                String.valueOf(colorBordersCB.isSelected()));
    }

    private void bottomBorderCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(
                CHART_DRAW_BOTTOM_BORDER,
                String.valueOf(bottomBorderCB.isSelected()));
    }

    private void topBorderCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(
                CHART_DRAW_TOP_BORDER,
                String.valueOf(topBorderCB.isSelected()));
    }

    private void showAllHeatmapFeauresCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(
                CHART_SHOW_ALL_HEATMAP,
                String.valueOf(showAllHeatmapFeauresCB.isSelected()));
    }


    private void chooseMutationColorsButtonActionPerformed(ActionEvent e) {
        PaletteColorTable ct = PreferencesManager.getPreferences().getMutationColorScheme();
        MutationColorMapEditor editor = new MutationColorMapEditor(IGV.getMainFrame(), ct.getColorMap(), IGV.getInstance().getSession().getColorOverlay());
        editor.setVisible(true);

        Map<String, Color> changedColors = editor.getChangedColors();
        if (!changedColors.isEmpty()) {
            for (Map.Entry<String, Color> entry : changedColors.entrySet()) {
                ct.getColorMap().put(entry.getKey(), entry.getValue());
            }
            String mapString = ct.getMapAsString();
            updatedPreferenceMap.put(MUTATION_COLOR_TABLE, mapString);
        }
    }


    private void colorMutationsCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(COLOR_MUTATIONS, String.valueOf(
                colorCodeMutationsCB.isSelected()));
    }


    private void checkForVCFColors() {

        Color homRefColor = homRefColorChooser.getSelectedColor();

        if (!homRefColor.equals(prefMgr.getAsColor(HOMREF_COLOR))) {
            updatedPreferenceMap.put(HOMREF_COLOR, ColorUtilities.colorToString(homRefColor));
        }

        Color hetVarColor = hetVarColorChooser.getSelectedColor();

        if (!homRefColor.equals(prefMgr.getAsColor(HETVAR_COLOR))) {
            updatedPreferenceMap.put(HETVAR_COLOR, ColorUtilities.colorToString(hetVarColor));
        }

        Color homVarColor = homVarColorChooser.getSelectedColor();

        if (!homRefColor.equals(prefMgr.getAsColor(HOMVAR_COLOR))) {
            updatedPreferenceMap.put(HOMVAR_COLOR, ColorUtilities.colorToString(homVarColor));
        }

        Color noCallColor = noCallColorChooser.getSelectedColor();

        if (!homRefColor.equals(prefMgr.getAsColor(NOCALL_COLOR))) {
            updatedPreferenceMap.put(NOCALL_COLOR, ColorUtilities.colorToString(noCallColor));
        }

        Color afRefColor = afRefColorChooser.getSelectedColor();

        if (!homRefColor.equals(prefMgr.getAsColor(AF_REF_COLOR))) {
            updatedPreferenceMap.put(AF_REF_COLOR, ColorUtilities.colorToString(afRefColor));
        }

        Color afVarColor = afVarColorChooser.getSelectedColor();

        if (!homRefColor.equals(prefMgr.getAsColor(AF_VAR_COLOR))) {
            updatedPreferenceMap.put(AF_VAR_COLOR, ColorUtilities.colorToString(afVarColor));
        }

        boolean alleleFreq = alleleFreqRB.isSelected();
        if (alleleFreq != prefMgr.getAsBoolean(VARIANT_COLOR_BY_ALLELE_FREQ)) {
            updatedPreferenceMap.put(VARIANT_COLOR_BY_ALLELE_FREQ, Boolean.toString(alleleFreq));
        }

    }

    private void resetVCFButtonActionPerformed(ActionEvent e) {
        for (String vcfKey : Arrays.asList(HOMREF_COLOR, HETVAR_COLOR, HOMVAR_COLOR,
                NOCALL_COLOR, AF_REF_COLOR, AF_VAR_COLOR, VARIANT_COLOR_BY_ALLELE_FREQ)) {
            prefMgr.remove(vcfKey);
        }
        resetVCFColorChoosers();
    }


    private void resetVCFColorChoosers() {
        homRefColorChooser.setSelectedColor(prefMgr.getAsColor(HOMREF_COLOR));
        hetVarColorChooser.setSelectedColor(prefMgr.getAsColor(HETVAR_COLOR));
        homVarColorChooser.setSelectedColor(prefMgr.getAsColor(HOMVAR_COLOR));
        noCallColorChooser.setSelectedColor(prefMgr.getAsColor(NOCALL_COLOR));
        afRefColorChooser.setSelectedColor(prefMgr.getAsColor(AF_REF_COLOR));
        afVarColorChooser.setSelectedColor(prefMgr.getAsColor(AF_VAR_COLOR));

        if (prefMgr.getAsBoolean(VARIANT_COLOR_BY_ALLELE_FREQ)) {
            alleleFreqRB.setSelected(true);
            alleleFractionRB.setSelected(false);
        } else {
            alleleFreqRB.setSelected(false);
            alleleFractionRB.setSelected(true);
        }
    }


    private void showOrphanedMutationsCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(SHOW_ORPHANED_MUTATIONS, String.valueOf(
                showOrphanedMutationsCB.isSelected()));
    }

    private void overlayTrackCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(OVERLAY_MUTATION_TRACKS, String.valueOf(
                overlayTrackCB.isSelected()));
        overlayAttributeTextField.setEnabled(overlayTrackCB.isSelected());
        showOrphanedMutationsCB.setEnabled(overlayTrackCB.isSelected());
        updateOverlays = true;
    }//GEN-LAST:event_overlayTrackCBActionPerformed


    private void overlayAttributeTextFieldFocusLost(java.awt.event.FocusEvent evt) {
        String attributeName = String.valueOf(overlayAttributeTextField.getText());
        if (attributeName != null) {
            attributeName = attributeName.trim();
        }
        updatedPreferenceMap.put(OVERLAY_ATTRIBUTE_KEY, attributeName);
        updateOverlays = true;
    }//GEN-LAST:event_overlayAttributeTextFieldFocusLost

    private void overlayAttributeTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(OVERLAY_ATTRIBUTE_KEY, String.valueOf(
                overlayAttributeTextField.getText()));
        updateOverlays = true;
        // TODO add your handling code here:
    }//GEN-LAST:event_overlayAttributeTextFieldActionPerformed

    private void defaultTrackHeightFieldFocusLost(java.awt.event.FocusEvent evt) {
        String defaultTrackHeight = String.valueOf(defaultChartTrackHeightField.getText());
        try {
            Integer.parseInt(defaultTrackHeight);
            updatedPreferenceMap.put(TRACK_HEIGHT_KEY, defaultTrackHeight);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Track height must be an integer number.");
        }
    }//GEN-LAST:event_defaultTrackHeightFieldFocusLost

    private void defaultTrackHeightFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String defaultTrackHeight = String.valueOf(defaultChartTrackHeightField.getText());
        try {
            Integer.parseInt(defaultTrackHeight);
            updatedPreferenceMap.put(TRACK_HEIGHT_KEY, defaultTrackHeight);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Track height must be an integer number.");
        }
    }


    private void trackNameAttributeFieldFocusLost(java.awt.event.FocusEvent evt) {
        String attributeName = String.valueOf(trackNameAttributeField.getText());
        if (attributeName != null) {
            attributeName = attributeName.trim();
        }
        updatedPreferenceMap.put(TRACK_ATTRIBUTE_NAME_KEY, attributeName);
    }

    private void trackNameAttributeFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String attributeName = String.valueOf(trackNameAttributeField.getText());
        if (attributeName != null) {
            attributeName = attributeName.trim();
        }
        updatedPreferenceMap.put(TRACK_ATTRIBUTE_NAME_KEY, attributeName);
    }

    private void defaultChartTrackHeightFieldFocusLost(java.awt.event.FocusEvent evt) {
        defaultChartTrackHeightFieldActionPerformed(null);
    }

    private void defaultChartTrackHeightFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String defaultTrackHeight = String.valueOf(defaultChartTrackHeightField.getText());
        try {
            Integer.parseInt(defaultTrackHeight);
            updatedPreferenceMap.put(CHART_TRACK_HEIGHT_KEY, defaultTrackHeight);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Track height must be an integer number.");
        }
    }


    private void geneListFlankingFieldFocusLost(FocusEvent e) {
        geneListFlankingFieldActionPerformed(null);
    }


    private void geneListFlankingFieldActionPerformed(ActionEvent e) {
        String flankingRegion = String.valueOf(geneListFlankingField.getText());
        try {
            Integer.parseInt(flankingRegion);
            updatedPreferenceMap.put(FLANKING_REGION, flankingRegion);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Flanking region must be an integer number.");
        }

    }


    private void showAttributesDisplayCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {
        boolean state = ((JCheckBox) evt.getSource()).isSelected();
        updatedPreferenceMap.put(SHOW_ATTRIBUTE_VIEWS_KEY, String.valueOf(state));
        IGV.getInstance().doShowAttributeDisplay(state);
    }

    private void combinePanelsCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(SHOW_SINGLE_TRACK_PANE_KEY, String.valueOf(
                combinePanelsCB.isSelected()));
    }

    private void showDefaultTrackAttributesCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(SHOW_DEFAULT_TRACK_ATTRIBUTES, String.valueOf(
                showDefaultTrackAttributesCB.isSelected()));
    }


    private void showRegionBoundariesCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(SHOW_REGION_BARS, String.valueOf(
                showRegionBoundariesCB.isSelected()));
    }

    private void filterVendorFailedReadsCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(
                SAM_FILTER_FAILED_READS,
                String.valueOf(filterFailedReadsCB.isSelected()));
    }


    private void samMinBaseQualityFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String baseQuality = samMinBaseQualityField.getText().trim();
        try {
            Integer.parseInt(baseQuality);
            updatedPreferenceMap.put(SAM_BASE_QUALITY_MIN, baseQuality);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Base quality must be an integer.");
        }
    }//GEN-LAST:event_samMinBaseQualityFieldActionPerformed

    private void samMinBaseQualityFieldFocusLost(java.awt.event.FocusEvent evt) {
        samMinBaseQualityFieldActionPerformed(null);
    }//GEN-LAST:event_samMinBaseQualityFieldFocusLost

    private void samMaxBaseQualityFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String baseQuality = samMaxBaseQualityField.getText().trim();
        try {
            Integer.parseInt(baseQuality);
            updatedPreferenceMap.put(SAM_BASE_QUALITY_MAX, baseQuality);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Base quality must be an integer.");
        }

    }//GEN-LAST:event_samMaxBaseQualityFieldActionPerformed

    private void samMaxBaseQualityFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_samMaxBaseQualityFieldFocusLost
        samMaxBaseQualityFieldActionPerformed(null);
    }//GEN-LAST:event_samMaxBaseQualityFieldFocusLost

    private void expMapToGeneCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expMapToGeneCBActionPerformed
        updatedPreferenceMap.put(PROBE_MAPPING_KEY, String.valueOf(expMapToGeneCB.isSelected()));

    }//GEN-LAST:event_expMapToGeneCBActionPerformed

    private void labelYAxisCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_labelYAxisCBActionPerformed
        updatedPreferenceMap.put(CHART_DRAW_Y_AXIS, String.valueOf(labelYAxisCB.isSelected()));
    }


    private void showAlignmentTrackCBActionPerformed(ActionEvent e) {
        final boolean coverageOnlyCBSelected = showAlignmentTrackCB.isSelected();
        updatedPreferenceMap.put(SAM_SHOW_ALIGNMENT_TRACK, String.valueOf(coverageOnlyCBSelected));
    }


    private void showCovTrackCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showCovTrackCBActionPerformed
        updatedPreferenceMap.put(SAM_SHOW_COV_TRACK, String.valueOf(showCovTrackCB.isSelected()));
    }

    private void portFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portFieldActionPerformed
        String portString = portField.getText().trim();
        try {
            Integer.parseInt(portString);
            updatedPreferenceMap.put(PORT_NUMBER, portString);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Port must be an integer.");
        }
    }

    private void portFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_portFieldFocusLost
        portFieldActionPerformed(null);
    }

    private void enablePortCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enablePortCBActionPerformed
        updatedPreferenceMap.put(PORT_ENABLED, String.valueOf(enablePortCB.isSelected()));
        portField.setEnabled(enablePortCB.isSelected());

    }

    private void expandCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expandCBActionPerformed
        updatedPreferenceMap.put(
                EXPAND_FEAUTRE_TRACKS,
                String.valueOf(expandCB.isSelected()));
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        IGVPreferences prefMgr = PreferencesManager.getPreferences();
        genomeServerURLTextField.setEnabled(true);
        genomeServerURLTextField.setText(Globals.DEFAULT_GENOME_URL);
        updatedPreferenceMap.put(GENOMES_SERVER_URL, null);
        dataServerURLTextField.setEnabled(true);
        dataServerURLTextField.setText(Globals.DEFAULT_DATA_URL);
        updatedPreferenceMap.put(DATA_SERVER_URL_KEY, null);
    }

    private void searchZoomCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchZoomCBActionPerformed
        updatedPreferenceMap.put(SEARCH_ZOOM, String.valueOf(searchZoomCB.isSelected()));
    }

    private void showDatarangeCBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showDatarangeCBActionPerformed
        updatedPreferenceMap.put(CHART_SHOW_DATA_RANGE, String.valueOf(showDatarangeCB.isSelected()));
    }

    private void showDatarangeCBFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_showDatarangeCBFocusLost
        showDatarangeCBActionPerformed(null);
    }

    private void snpThresholdFieldActionPerformed(java.awt.event.ActionEvent evt) {
        String snpThreshold = snpThresholdField.getText().trim();
        try {
            Double.parseDouble(snpThreshold);
            updatedPreferenceMap.put(SAM_ALLELE_THRESHOLD, snpThreshold);
        } catch (NumberFormatException numberFormatException) {
            inputValidated = false;
            MessageUtils.showMessage("Allele frequency threshold must be a number.");
        }
    }

    private void snpThresholdFieldFocusLost(java.awt.event.FocusEvent evt) {
        snpThresholdFieldActionPerformed(null);
    }

    private void autoFileDisoveryCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(BYPASS_FILE_AUTO_DISCOVERY, String.valueOf(!autoFileDisoveryCB.isSelected()));
    }

    private void normalizeCoverageCBActionPerformed(java.awt.event.ActionEvent evt) {
        updatedPreferenceMap.put(NORMALIZE_COVERAGE, String.valueOf(normalizeCoverageCB.isSelected()));
        portField.setEnabled(enablePortCB.isSelected());

    }


    // Proxy settings


    private void clearProxySettingsButtonActionPerformed(ActionEvent e) {
        if (MessageUtils.confirm("This will immediately clear all proxy settings.  Are you sure?")) {
            this.proxyHostField.setText("");
            this.proxyPortField.setText("");
            this.proxyUsernameField.setText("");
            this.proxyPasswordField.setText("");
            this.proxyTypeCB.setSelectedIndex(0);
            this.useProxyCB.setSelected(false);
            PreferencesManager.getPreferences().clearProxySettings();
        }
    }


    private void useProxyCBActionPerformed(java.awt.event.ActionEvent evt) {
        proxySettingsChanged = true;
        boolean useProxy = useProxyCB.isSelected();
        boolean authenticateProxy = authenticateProxyCB.isSelected();
        portField.setEnabled(enablePortCB.isSelected());
        updateProxyState(useProxy, authenticateProxy);
        updatedPreferenceMap.put(USE_PROXY, String.valueOf(useProxy));

    }


    private void authenticateProxyCBActionPerformed(java.awt.event.ActionEvent evt) {
        proxySettingsChanged = true;
        boolean useProxy = useProxyCB.isSelected();
        boolean authenticateProxy = authenticateProxyCB.isSelected();
        portField.setEnabled(enablePortCB.isSelected());
        updateProxyState(useProxy, authenticateProxy);
        updatedPreferenceMap.put(PROXY_AUTHENTICATE, String.valueOf(authenticateProxy));

        proxyUsernameField.setEnabled(authenticateProxy);
        proxyPasswordField.setEnabled(authenticateProxy);

    }


    private void proxyHostFieldFocusLost(java.awt.event.FocusEvent evt) {
        proxyHostFieldActionPerformed(null);
    }

    private void proxyHostFieldActionPerformed(java.awt.event.ActionEvent evt) {
        proxySettingsChanged = true;
        updatedPreferenceMap.put(PROXY_HOST, proxyHostField.getText());
    }

    private void proxyPortFieldFocusLost(java.awt.event.FocusEvent evt) {
        proxyPortFieldActionPerformed(null);
    }

    private void proxyPortFieldActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Integer.parseInt(proxyPortField.getText());
            proxySettingsChanged = true;
            updatedPreferenceMap.put(PROXY_PORT, proxyPortField.getText());
        } catch (NumberFormatException e) {
            MessageUtils.showMessage("Proxy port must be an integer.");
        }
    }


    private void proxyWhitelistTextAreaActionPerformed(java.awt.event.ActionEvent evt) {

        String[] urls = proxyWhitelistTextArea.getText().split("\n");
        String setting = "";
        for (String u : urls) {
            setting += u + ",";
        }
        updatedPreferenceMap.put(PROXY_WHITELIST, setting);
        proxySettingsChanged = true;

    }


    private void proxyWhitelistTextAreaFocusLost(FocusEvent e) {
        proxyWhitelistTextAreaActionPerformed(null);
    }


    private void proxyTypeCBActionPerformed(ActionEvent e) {
        proxySettingsChanged = true;
        String proxyTypeString = proxyTypeCB.getSelectedItem().toString();
        updatedPreferenceMap.put(PROXY_TYPE, proxyTypeString);
    }


    // Username

    private void proxyUsernameFieldFocusLost(java.awt.event.FocusEvent evt) {
        proxyUsernameFieldActionPerformed(null);
    }

    private void proxyUsernameFieldActionPerformed(java.awt.event.ActionEvent evt) {
        proxySettingsChanged = true;
        String user = proxyUsernameField.getText();
        updatedPreferenceMap.put(PROXY_USER, user);

    }

    // Password

    private void proxyPasswordFieldFocusLost(java.awt.event.FocusEvent evt) {
        proxyPasswordFieldActionPerformed(null);
    }

    private void proxyPasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {
        proxySettingsChanged = true;
        String pw = new String(proxyPasswordField.getPassword());
        String pwEncoded = Utilities.base64Encode(pw);
        updatedPreferenceMap.put(PROXY_PW, pwEncoded);

    }


    private void updateProxyState(boolean useProxy, boolean authenticateProxy) {
        proxyHostField.setEnabled(useProxy);
        proxyPortField.setEnabled(useProxy);
        proxyUsernameField.setEnabled(useProxy && authenticateProxy);
        proxyPasswordField.setEnabled(useProxy && authenticateProxy);
    }

    private void resetValidation() {
        // Assume valid input until proven otherwise
        inputValidated = true;
    }

    private void dbHostFieldFocusLost(FocusEvent e) {
        dbHostFieldActionPerformed(null);
    }

    private void dbHostFieldActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(DB_HOST, dbHostField.getText());
    }

    private void dbNameFieldFocusLost(FocusEvent e) {
        dbNameFieldActionPerformed(null);
    }

    private void dbNameFieldActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(DB_NAME, dbNameField.getText());
    }

    private void dbPortFieldActionPerformed(ActionEvent e) {
        dbPortFieldFocusLost(null);
    }

    private void dbPortFieldFocusLost(FocusEvent e) {

        String portText = dbPortField.getText().trim();
        if (portText.length() == 0) {
            updatedPreferenceMap.put(DB_PORT, "-1");
        } else {
            try {
                Integer.parseInt(portText);
                updatedPreferenceMap.put(DB_PORT, portText);
            } catch (NumberFormatException e1) {
                updatedPreferenceMap.put(DB_PORT, "-1");
            }
        }

    }


    private void probeMappingBrowseButtonActionPerformed(ActionEvent e) {
        File f = FileDialogUtils.chooseFile("Probe mapping file (BED format)");
        if (f != null) {
            probeMappingFileTextField.setText(f.getAbsolutePath());
            updatedPreferenceMap.put(PROBE_MAPPING_FILE, f.getAbsolutePath());
        }

    }


    private void useProbeMappingCBActionPerformed(ActionEvent e) {
        boolean isSelected = useProbeMappingCB.isSelected();
        updatedPreferenceMap.put(USE_PROBE_MAPPING_FILE, String.valueOf(isSelected));
        updateProbeMappingOptions(isSelected);
    }


    private void updateProbeMappingOptions(boolean isSelected) {
        probeMappingFileTextField.setEnabled(isSelected);
        probeMappingBrowseButton.setEnabled(isSelected);
        expMapToGeneCB.setEnabled(!isSelected);
        expMapToLociCB.setEnabled(!isSelected);
    }


    private void probeMappingFileTextFieldFocusLost(FocusEvent e) {
        probeMappingFileTextFieldActionPerformed(null);
    }


    private void probeMappingFileTextFieldActionPerformed(ActionEvent e) {
        String name = probeMappingFileTextField.getText();
        if (name != null) {
            name = name.trim();
            updatedPreferenceMap.put(PROBE_MAPPING_FILE, name);
        }

    }

    private void toolTipInitialDelayFieldFocusLost(FocusEvent e) {
        toolTipInitialDelayFieldActionPerformed(null);
    }


    private void toolTipInitialDelayFieldActionPerformed(ActionEvent e) {
        String ttText = toolTipInitialDelayField.getText();
        try {
            Integer.parseInt(ttText);
            updatedPreferenceMap.put(TOOLTIP_INITIAL_DELAY, ttText);
            tooltipSettingsChanged = true;
        } catch (NumberFormatException e1) {
            MessageUtils.showMessage("Tooltip initial delay must be a number.");
        }
    }

    private void tooltipReshowDelayFieldFocusLost(FocusEvent e) {
        tooltipReshowDelayFieldActionPerformed(null);
    }

    private void tooltipReshowDelayFieldActionPerformed(ActionEvent e) {
        String ttText = tooltipReshowDelayField.getText();
        try {
            Integer.parseInt(ttText);
            updatedPreferenceMap.put(TOOLTIP_RESHOW_DELAY, ttText);
            tooltipSettingsChanged = true;
        } catch (NumberFormatException e1) {
            MessageUtils.showMessage("Tooltip reshow delay must be a number.");
        }

    }


    private void tooltipDismissDelayFieldFocusLost(FocusEvent e) {
        tooltipDismissDelayFieldActionPerformed(null);
    }

    private void tooltipDismissDelayFieldActionPerformed(ActionEvent e) {
        String ttText = tooltipDismissDelayField.getText();
        try {
            Integer.parseInt(ttText);
            updatedPreferenceMap.put(TOOLTIP_DISMISS_DELAY, ttText);
            tooltipSettingsChanged = true;
        } catch (NumberFormatException e1) {
            MessageUtils.showMessage("Tooltip dismiss delay must be a number.");
        }

    }


    private void scaleFontsCBActionPerformed(ActionEvent e) {
        PreferencesManager.getPreferences().put(SCALE_FONTS, scaleFontsCB.isSelected());
    }

    private void enableGoogleCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(ENABLE_GOOGLE_MENU, String.valueOf(enableGoogleCB.isSelected()));
    }

    private void saveGoogleCredentialsCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(SAVE_GOOGLE_CREDENTIALS, String.valueOf(saveGoogleCredentialsCB.isSelected()));
    }

    private void sessionPathsCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(SESSION_RELATIVE_PATH, String.valueOf(sessionPathsCB.isSelected()));
    }

    private void coverageOnlyCBActionPerformed(ActionEvent e) {
        // TODO add your code here
    }


    private void cramCacheSizeFieldFocusLost(FocusEvent e) {
        cramCacheSizeFieldActionPerformed(null);
    }

    private void cramCacheSizeFieldActionPerformed(ActionEvent e) {
        try {
            String p = cramCacheSizeField.getText();
            Float.parseFloat(p);
            updatedPreferenceMap.put(CRAM_CACHE_SIZE, p);
        }
        catch(NumberFormatException ex) {
            MessageUtils.showMessage("Cache size must be a number");
            cramCacheSizeField.setText(prefMgr.get(CRAM_CACHE_SIZE));
        }
    }


    private void cramCacheDirectoryButtonActionPerformed(ActionEvent e) {
        cramCacheDirectory = DirectoryManager.getFastaCacheDirectory();
        final File newDirectory = FileDialogUtils.chooseDirectory("Select IGV directory", DirectoryManager.getUserDirectory());
        if (newDirectory != null && !newDirectory.equals(cramCacheDirectory.getParentFile())) {
            newCramCacheDirectory = new File(newDirectory, "igv");
            cramCacheDirectoryField.setText(newCramCacheDirectory.getAbsolutePath());
        }
    }

    private void cramCacheReferenceCBActionPerformed(ActionEvent e) {
        updatedPreferenceMap.put(CRAM_CACHE_SEQUENCES, String.valueOf(cramCacheReferenceCB.isSelected()));
    }


    /*
   *    Object selection = geneMappingFile.getSelectedItem();
  String filename = (selection == null ? null : selection.toString().trim());
  updatedPreferenceMap.put(
  IGVPreferences.USER_PROBE_MAP_KEY,
  filename);
   * */

    private void initValues() {
        combinePanelsCB.setSelected(prefMgr.getAsBoolean(SHOW_SINGLE_TRACK_PANE_KEY));
        //drawExonNumbersCB.setSelected(preferenceManager.getDrawExonNumbers());

        showRegionBoundariesCB.setSelected(prefMgr.getAsBoolean(SHOW_REGION_BARS));
        defaultChartTrackHeightField.setText(prefMgr.get(CHART_TRACK_HEIGHT_KEY));
        defaultTrackHeightField.setText(prefMgr.get(TRACK_HEIGHT_KEY));
        showOrphanedMutationsCB.setSelected(prefMgr.getAsBoolean(SHOW_ORPHANED_MUTATIONS));
        overlayAttributeTextField.setText(prefMgr.get(OVERLAY_ATTRIBUTE_KEY));
        overlayTrackCB.setSelected(prefMgr.getAsBoolean(OVERLAY_MUTATION_TRACKS));
        showDefaultTrackAttributesCB.setSelected(prefMgr.getAsBoolean(SHOW_DEFAULT_TRACK_ATTRIBUTES));
        colorCodeMutationsCB.setSelected(prefMgr.getAsBoolean(COLOR_MUTATIONS));
        overlayAttributeTextField.setEnabled(overlayTrackCB.isSelected());
        showOrphanedMutationsCB.setEnabled(overlayTrackCB.isSelected());
        seqResolutionThreshold.setText(prefMgr.get(MAX_SEQUENCE_RESOLUTION));

        scaleFontsCB.setSelected(prefMgr.getAsBoolean(SCALE_FONTS));

        enableGoogleCB.setSelected(prefMgr.getAsBoolean(ENABLE_GOOGLE_MENU));
        saveGoogleCredentialsCB.setSelected(prefMgr.getAsBoolean(SAVE_GOOGLE_CREDENTIALS));

        sessionPathsCB.setSelected(prefMgr.getAsBoolean(SESSION_RELATIVE_PATH));

        geneListFlankingField.setText(prefMgr.get(FLANKING_REGION));

        enablePortCB.setSelected(prefMgr.getAsBoolean(PORT_ENABLED));
        portField.setText(String.valueOf(prefMgr.getAsInt(PORT_NUMBER)));
        portField.setEnabled(enablePortCB.isSelected());

        expandCB.setSelected(prefMgr.getAsBoolean(EXPAND_FEAUTRE_TRACKS));
        searchZoomCB.setSelected(prefMgr.getAsBoolean(SEARCH_ZOOM));

        showAttributesDisplayCheckBox.setSelected(prefMgr.getAsBoolean(SHOW_ATTRIBUTE_VIEWS_KEY));
        trackNameAttributeField.setText(prefMgr.get(TRACK_ATTRIBUTE_NAME_KEY));

        genomeServerURLTextField.setText(prefMgr.getGenomeListURL());
        dataServerURLTextField.setText(prefMgr.getDataServerURL());

        blatURLField.setText(prefMgr.get(BLAT_URL));

        // Chart panel
        topBorderCB.setSelected(prefMgr.getAsBoolean(CHART_DRAW_TOP_BORDER));
        bottomBorderCB.setSelected(prefMgr.getAsBoolean(CHART_DRAW_BOTTOM_BORDER));
        colorBordersCB.setSelected(prefMgr.getAsBoolean(CHART_COLOR_BORDERS));
        chartDrawTrackNameCB.setSelected(prefMgr.getAsBoolean(CHART_DRAW_TRACK_NAME));
        autoscaleCB.setSelected(prefMgr.getAsBoolean(CHART_AUTOSCALE));
        showDatarangeCB.setSelected(prefMgr.getAsBoolean(CHART_SHOW_DATA_RANGE));
        labelYAxisCB.setSelected(prefMgr.getAsBoolean(CHART_DRAW_Y_AXIS));
        showAllHeatmapFeauresCB.setSelected(prefMgr.getAsBoolean(CHART_SHOW_ALL_HEATMAP));

        samMaxWindowSizeField.setText(prefMgr.get(SAM_MAX_VISIBLE_RANGE));
        samSamplingWindowField.setText(prefMgr.get(SAM_SAMPLING_WINDOW));
        samDownsampleCountField.setText(prefMgr.get(SAM_SAMPLING_COUNT));

        boolean downsample = prefMgr.getAsBoolean(SAM_DOWNSAMPLE_READS);
        downsampleReadsCB.setSelected(downsample);
        samSamplingWindowField.setEnabled(downsample);
        samDownsampleCountField.setEnabled(downsample);

        mappingQualityThresholdField.setText(prefMgr.get(SAM_QUALITY_THRESHOLD));
        insertSizeThresholdField.setText(prefMgr.get(SAM_MAX_INSERT_SIZE_THRESHOLD));
        insertSizeMinThresholdField.setText(prefMgr.get(SAM_MIN_INSERT_SIZE_THRESHOLD));
        insertSizeMinPercentileField.setText(prefMgr.get(SAM_MIN_INSERT_SIZE_PERCENTILE));
        insertSizeMaxPercentileField.setText(prefMgr.get(SAM_MAX_INSERT_SIZE_PERCENTILE));

        final boolean isizeComputeSelected = prefMgr.getAsBoolean(SAM_COMPUTE_ISIZES);
        isizeComputeCB.setSelected(isizeComputeSelected);
        insertSizeThresholdField.setEnabled(!isizeComputeSelected);
        insertSizeMinThresholdField.setEnabled(!isizeComputeSelected);
        insertSizeMinPercentileField.setEnabled(isizeComputeSelected);
        insertSizeMaxPercentileField.setEnabled(isizeComputeSelected);

        snpThresholdField.setText((String.valueOf(prefMgr.getAsFloat(SAM_ALLELE_THRESHOLD))));
        //samShowZeroQualityCB.setSelected(samPrefs.isShowZeroQuality());
        useAlleleQualityCB.setSelected(prefMgr.getAsBoolean(SAM_ALLELE_USE_QUALITY));
        samFilterDuplicatesCB.setSelected(!prefMgr.getAsBoolean(SAM_SHOW_DUPLICATES));
        filterFailedReadsCB.setSelected(prefMgr.getAsBoolean(SAM_FILTER_FAILED_READS));
        filterSecondaryAlignmentsCB.setSelected(prefMgr.getAsBoolean(SAM_FILTER_SECONDARY_ALIGNMENTS));
        filterSupplementaryAlignmentsCB.setSelected(prefMgr.getAsBoolean(SAM_FILTER_SUPPLEMENTARY_ALIGNMENTS));
        showSoftClippedCB.setSelected(prefMgr.getAsBoolean(SAM_SHOW_SOFT_CLIPPED));
        quickConsensusModeCB.setSelected(prefMgr.getAsBoolean(SAM_QUICK_CONSENSUS_MODE));
        samFlagUnmappedPairCB.setSelected(prefMgr.getAsBoolean(SAM_FLAG_UNMAPPED_PAIR));
        showCenterLineCB.setSelected(prefMgr.getAsBoolean(SAM_SHOW_CENTER_LINE));
        samShadeMismatchedBaseCB.setSelected(ShadeBasesOption.QUALITY ==
                CollUtils.valueOf(ShadeBasesOption.class, prefMgr.get(SAM_SHADE_BASES), ShadeBasesOption.QUALITY));
        samMinBaseQualityField.setText((String.valueOf(prefMgr.getAsInt(SAM_BASE_QUALITY_MIN))));
        samMaxBaseQualityField.setText((String.valueOf(prefMgr.getAsInt(SAM_BASE_QUALITY_MAX))));
        samMinBaseQualityField.setEnabled(samShadeMismatchedBaseCB.isSelected());
        samMaxBaseQualityField.setEnabled(samShadeMismatchedBaseCB.isSelected());
        showCovTrackCB.setSelected(prefMgr.getAsBoolean(SAM_SHOW_COV_TRACK));

        String samHiddenTags = prefMgr.get(SAM_HIDDEN_TAGS), samHiddenTagsClean = "";
        for (String s : (samHiddenTags == null ? "" : samHiddenTags).split("[, ]")) {
            if (!s.equals("")) {
                samHiddenTagsClean += (samHiddenTagsClean.equals("") ? "" : ",") + s;
            }
        }
        samHiddenTagsField.setText(samHiddenTagsClean);

        final boolean junctionTrackEnabled = prefMgr.getAsBoolean(SAM_SHOW_JUNCTION_TRACK);
        showJunctionTrackCB.setSelected(junctionTrackEnabled);
        showJunctionFlankingRegionsCB.setSelected(prefMgr.getAsBoolean(SAM_SHOW_JUNCTION_FLANKINGREGIONS));
        junctionFlankingTextField.setText(prefMgr.get(SAM_JUNCTION_MIN_FLANKING_WIDTH));
        junctionCoverageTextField.setText(prefMgr.get(SAM_JUNCTION_MIN_COVERAGE));


        genomeUpdateCB.setSelected(prefMgr.getAsBoolean(AUTO_UPDATE_GENOMES));
        antialiasingCB.setSelected(prefMgr.getAsBoolean(ENABLE_ANTIALISING));

        final boolean mapProbesToGenes = PreferencesManager.getPreferences().getAsBoolean(PROBE_MAPPING_KEY);
        expMapToGeneCB.setSelected(mapProbesToGenes);
        expMapToLociCB.setSelected(!mapProbesToGenes);

        probeMappingFileTextField.setText(prefMgr.get(PROBE_MAPPING_FILE));
        boolean useProbeMapping = prefMgr.getAsBoolean(USE_PROBE_MAPPING_FILE);
        useProbeMappingCB.setSelected(useProbeMapping);
        updateProbeMappingOptions(useProbeMapping);

        autoFileDisoveryCB.setSelected(!prefMgr.getAsBoolean(BYPASS_FILE_AUTO_DISCOVERY));
        normalizeCoverageCB.setSelected(prefMgr.getAsBoolean(NORMALIZE_COVERAGE));


        boolean useProxy = prefMgr.getAsBoolean(USE_PROXY);
        useProxyCB.setSelected(useProxy);

        boolean authenticateProxy = prefMgr.getAsBoolean(PROXY_AUTHENTICATE);
        authenticateProxyCB.setSelected(authenticateProxy);

        proxyHostField.setText(prefMgr.get(PROXY_HOST, ""));
        proxyPortField.setText(prefMgr.get(PROXY_PORT, ""));
        proxyUsernameField.setText(prefMgr.get(PROXY_USER, ""));
        String pwCoded = prefMgr.get(PROXY_PW, "");
        proxyPasswordField.setText(Utilities.base64Decode(pwCoded));
        proxyWhitelistTextArea.setText(prefMgr.get(PROXY_WHITELIST));

        String proxyTypeString = prefMgr.get(PROXY_TYPE, null);
        if (proxyTypeString != null) {
            proxyTypeCB.setSelectedItem(proxyTypeString);
        }

        backgroundColorPanel.setBackground(
                PreferencesManager.getPreferences().getAsColor(BACKGROUND_COLOR));

        dbHostField.setText(prefMgr.get(DB_HOST));
        dbNameField.setText(prefMgr.get(DB_NAME));
        String portText = prefMgr.get(DB_PORT);
        if (!portText.equals("-1")) {
            dbPortField.setText(portText);
        }


        final File igvDirectory = DirectoryManager.getIgvDirectory();
        if (igvDirectory != null) {
            igvDirectoryField.setText(igvDirectory.getAbsolutePath());
        }

        tooltipDismissDelayField.setText(prefMgr.get(TOOLTIP_DISMISS_DELAY));
        tooltipReshowDelayField.setText(prefMgr.get(TOOLTIP_RESHOW_DELAY));
        toolTipInitialDelayField.setText(prefMgr.get(TOOLTIP_INITIAL_DELAY));

        featureVisibilityWindowField.setText(prefMgr.get(DEFAULT_VISIBILITY_WINDOW));

        showAlignmentTrackCB.setSelected(prefMgr.getAsBoolean(SAM_SHOW_ALIGNMENT_TRACK));

        samFlagIndelsCB.setSelected(prefMgr.getAsBoolean(SAM_FLAG_LARGE_INDELS));
        samFlagIndelsThresholdField.setText(prefMgr.get(SAM_LARGE_INDELS_THRESHOLD));
        samFlagIndelsThresholdField.setEnabled(samFlagIndelsCB.isSelected());

        samFlagClippingCB.setSelected(prefMgr.getAsBoolean(SAM_FLAG_CLIPPING));
        samFlagClippingThresholdField.setText(prefMgr.get(SAM_CLIPPING_THRESHOLD));
        samFlagClippingThresholdField.setEnabled(samFlagClippingCB.isSelected());

        hideIndelsBasesCB.setSelected(prefMgr.getAsBoolean(SAM_HIDE_SMALL_INDEL));
        hideIndelsBasesField.setText(prefMgr.get(SAM_SMALL_INDEL_BP_THRESHOLD));
        hideIndelsBasesField.setEnabled(hideIndelsBasesCB.isSelected());

        cramCacheReferenceCB.setSelected(prefMgr.getAsBoolean(CRAM_CACHE_SEQUENCES));
        final File cramCacheDirectory = DirectoryManager.getFastaCacheDirectory();
        if (cramCacheDirectory != null) {
            cramCacheDirectoryField.setText(cramCacheDirectory.getAbsolutePath());
        }
        cramCacheSizeField.setText(prefMgr.get(CRAM_CACHE_SIZE));


        resetVCFColorChoosers();

        updateFontField();

        updateProxyState(useProxy, authenticateProxy);
    }

    private void updateFontField() {
        Font font = FontManager.getDefaultFont();
        StringBuffer buf = new StringBuffer();
        buf.append(font.getFamily());
        if (font.isBold()) {
            buf.append(" bold");
        }
        if (font.isItalic()) {
            buf.append(" italic");
        }
        buf.append(" " + font.getSize());
        defaultFontField.setText(buf.toString());

    }


    private void checkForProbeChanges() {
        if (updatedPreferenceMap.containsKey(PROBE_MAPPING_KEY)) {
            ProbeToLocusMap.getInstance().clearProbeMappings();
        }
    }


    /**
     * Move the IGV directory to a new location.
     */
    private void moveIGVDirectory() {

        // DO this in a swing worker, so we can invoke a wait cursor.  This might take some time.
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                return DirectoryManager.moveIGVDirectory(newIGVDirectory);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        };


        worker.execute();
        try {
            Boolean success = worker.get(30, TimeUnit.SECONDS);
            if (success == Boolean.TRUE) {
                MessageUtils.showMessage("<html>The IGV directory has been successfully moved to: " +
                        newIGVDirectory.getAbsolutePath() +
                        "<br>Some files might need to be manually removed from the previous directory." +
                        "<br/><b><i>It is recommended that you restart IGV.");
            }

        } catch (Exception ex) {
            MessageUtils.showMessage("<html>Unexpected error occurred while moving IGV directory:  " +
                    newIGVDirectory.getAbsolutePath() + " " + ex.getMessage() +
                    "<br/><b><i>It is recommended that you restart IGV.");

        }
    }


    /**
     * Move the CRAM sequence cache directory to a new location.
     */
    private void moveCramCacheDirectory() {

        if(cramCacheDirectory != null && cramCacheDirectory.exists() && cramCacheDirectory.isDirectory() &&
                newCramCacheDirectory != null && newCramCacheDirectory.exists() && newCramCacheDirectory.isDirectory()) {

            for(File f : cramCacheDirectory.listFiles()) {
                Path p1 = f.toPath();
                Path p2 = (new File(newCramCacheDirectory, f.getName())).toPath();
                try {
                    Files.move(p1, p2);
                } catch (IOException e) {
                    log.error("Error moving cached sequence file", e);
                }
            }

        }
    }





    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                PreferencesEditor dialog = new PreferencesEditor(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    //TODO move this to another class,  or resource bundle
    static String overlayText = "<html>These options control the treatment of mutation tracks.  " +
            "Mutation data may optionally<br>be overlaid on other tracks that have a matching attribute value " +
            "from the sample info <br>file. " +
            "This is normally an attribute that identifies a sample or patient. The attribute key <br>is specified in the" +
            "text field below.";

    public String getOverlayText() {
        return overlayText;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JScrollPane panel7;
    private JPanel panel6;
    private JTabbedPane tabbedPane;
    private JScrollPane panel22;
    private JPanel generalPanel;
    private JPanel vSpacer7;
    private JPanel jPanel10;
    private JCheckBox sessionPathsCB;
    private JLabel missingDataExplanation;
    private JCheckBox showDefaultTrackAttributesCB;
    private JCheckBox combinePanelsCB;
    private JCheckBox showAttributesDisplayCheckBox;
    private JCheckBox searchZoomCB;
    private JLabel label4;
    private JTextField geneListFlankingField;
    private JLabel zoomToFeatureExplanation2;
    private JLabel label6;
    private JTextField seqResolutionThreshold;
    private JLabel label10;
    private JButton fontChangeButton;
    private JCheckBox showRegionBoundariesCB;
    private JLabel label7;
    private JPanel backgroundColorPanel;
    private JCheckBox enableGoogleCB;
    private JLabel label33;
    private JCheckBox saveGoogleCredentialsCB;
    private JLabel label34;
    private JLabel textField1;
    private JTextField featureVisibilityWindowField;
    private JLabel zoomToFeatureExplanation3;
    private JTextField defaultFontField;
    private JButton resetFontButton;
    private JCheckBox scaleFontsCB;
    private JLabel label8;
    private JButton resetBackgroundButton;
    private JScrollPane panel23;
    private JPanel tracksPanel;
    private JPanel vSpacer1;
    private JLabel jLabel5;
    private JTextField defaultChartTrackHeightField;
    private JLabel trackNameAttributeLabel;
    private JTextField trackNameAttributeField;
    private JLabel jLabel8;
    private JTextField defaultTrackHeightField;
    private JPanel hSpacer1;
    private JCheckBox expandCB;
    private JCheckBox normalizeCoverageCB;
    private JLabel missingDataExplanation8;
    private JScrollPane panel24;
    private JPanel overlaysPanel;
    private JPanel jPanel5;
    private JLabel jLabel3;
    private JTextField overlayAttributeTextField;
    private JCheckBox overlayTrackCB;
    private JLabel jLabel2;
    private JLabel jLabel4;
    private JCheckBox colorCodeMutationsCB;
    private JButton chooseMutationColorsButton;
    private JLabel label11;
    private JCheckBox showOrphanedMutationsCB;
    private JLabel label12;
    private JPanel panel33;
    private JLabel label36;
    private ColorChooserPanel homRefColorChooser;
    private JLabel label38;
    private ColorChooserPanel homVarColorChooser;
    private JLabel label37;
    private ColorChooserPanel hetVarColorChooser;
    private JLabel label40;
    private ColorChooserPanel noCallColorChooser;
    private JLabel label41;
    private ColorChooserPanel afRefColorChooser;
    private JLabel label42;
    private ColorChooserPanel afVarColorChooser;
    private JButton resetVCFButton;
    private JPanel panel35;
    private JLabel label43;
    private JRadioButton alleleFreqRB;
    private JRadioButton alleleFractionRB;
    private JScrollPane panel25;
    private JPanel chartPanel;
    private JPanel jPanel4;
    private JCheckBox topBorderCB;
    private JLabel label1;
    private JCheckBox chartDrawTrackNameCB;
    private JCheckBox bottomBorderCB;
    private JLabel jLabel7;
    private JCheckBox colorBordersCB;
    private JCheckBox labelYAxisCB;
    private JCheckBox autoscaleCB;
    private JLabel jLabel9;
    private JCheckBox showDatarangeCB;
    private JPanel panel1;
    private JLabel label13;
    private JCheckBox showAllHeatmapFeauresCB;
    private JLabel label14;
    private JScrollPane panel20;
    private JPanel alignmentPanel;
    private JPanel jPanel11;
    private JPanel panel32;
    private JLabel label39;
    private JCheckBox showAlignmentTrackCB;
    private JCheckBox showCovTrackCB;
    private JCheckBox showJunctionTrackCB;
    private JPanel jPanel12;
    private JPanel panel13;
    private JPanel panel31;
    private JLabel jLabel11;
    private JTextField samMaxWindowSizeField;
    private JLabel jLabel12;
    private JPanel panel4;
    private JCheckBox downsampleReadsCB;
    private JPanel hSpacer3;
    private JLabel label23;
    private JTextField samDownsampleCountField;
    private JLabel jLabel13;
    private JTextField samSamplingWindowField;
    private JPanel panel11;
    private JCheckBox samShadeMismatchedBaseCB;
    private JTextField samMinBaseQualityField;
    private JLabel label2;
    private JTextField samMaxBaseQualityField;
    private JPanel panel12;
    private JLabel jLabel15;
    private JTextField mappingQualityThresholdField;
    private JPanel panel10;
    private JCheckBox samFlagIndelsCB;
    private JTextField samFlagIndelsThresholdField;
    private JLabel label31;
    private JPanel panel10clip;
    private JCheckBox samFlagClippingCB;
    private JTextField samFlagClippingThresholdField;
    private JLabel label31clip;
    private JPanel panel9;
    private JCheckBox hideIndelsBasesCB;
    private JTextField hideIndelsBasesField;
    private JLabel label45;
    private JPanel panel8;
    private JCheckBox samFilterDuplicatesCB;
    private JCheckBox samFlagUnmappedPairCB;
    private JCheckBox filterFailedReadsCB;
    private JCheckBox showSoftClippedCB;
    private JCheckBox filterSecondaryAlignmentsCB;
    private JCheckBox quickConsensusModeCB;
    private JCheckBox showCenterLineCB;
    private JCheckBox filterSupplementaryAlignmentsCB;
    private JPanel panel31b;
    private JLabel jLabel11b;
    private JTextField samHiddenTagsField;
    private JPanel vSpacer5;
    private JPanel panel34;
    private JPanel panel5;
    private JLabel jLabel26;
    private JTextField snpThresholdField;
    private JPanel hSpacer2;
    private JCheckBox useAlleleQualityCB;
    private JPanel panel3;
    private JCheckBox showJunctionFlankingRegionsCB;
    private JLabel label15;
    private JTextField junctionFlankingTextField;
    private JLabel label16;
    private JTextField junctionCoverageTextField;
    private JPanel vSpacer6;
    private JPanel panel2;
    private JPanel panel19;
    private JPanel panel16;
    private JLabel label9;
    private JLabel jLabel20;
    private JTextField insertSizeMinThresholdField;
    private JLabel jLabel17;
    private JTextField insertSizeThresholdField;
    private JPanel panel15;
    private JCheckBox isizeComputeCB;
    private JLabel jLabel30;
    private JTextField insertSizeMinPercentileField;
    private JLabel jLabel18;
    private JTextField insertSizeMaxPercentileField;
    private JScrollPane panel26;
    private JPanel expressionPane;
    private JPanel jPanel8;
    private JPanel panel18;
    private JLabel jLabel24;
    private JLabel jLabel21;
    private JRadioButton expMapToLociCB;
    private JRadioButton expMapToGeneCB;
    private JPanel panel17;
    private JCheckBox useProbeMappingCB;
    private JLabel label22;
    private JPanel panel14;
    private JTextField probeMappingFileTextField;
    private JButton probeMappingBrowseButton;
    private JScrollPane panel27;
    private JPanel proxyPanel;
    private JPanel jPanel15;
    private JLabel label3;
    private JButton clearProxySettingsButton;
    private JTextField proxyUsernameField;
    private JLabel jLabel28;
    private JCheckBox authenticateProxyCB;
    private JLabel jLabel29;
    private JPasswordField proxyPasswordField;
    private JTextField proxyHostField;
    private JTextField proxyPortField;
    private JLabel jLabel27;
    private JLabel jLabel23;
    private JCheckBox useProxyCB;
    private JComboBox<String> proxyTypeCB;
    private JLabel label27;
    private JLabel label35;
    private JTextField proxyWhitelistTextArea;
    private JScrollPane panel30;
    private JPanel dbPanel;
    private JLabel label20;
    private JPanel panel21;
    private JLabel label17;
    private JLabel label19;
    private JTextField dbNameField;
    private JTextField dbHostField;
    private JLabel label18;
    private JTextField dbPortField;
    private JScrollPane panel29;
    private JPanel advancedPanel;
    private JButton clearGenomeCacheButton;
    private JCheckBox enablePortCB;
    private JTextField portField;
    private JLabel jLabel22;
    private JPanel vSpacer12;
    private JCheckBox genomeUpdateCB;
    private JLabel jLabel6;
    private JTextField dataServerURLTextField;
    private JLabel jLabel1;
    private JTextField genomeServerURLTextField;
    private JCheckBox editServerPropertiesCB;
    private JButton jButton1;
    private JPanel vSpacer11;
    private JCheckBox autoFileDisoveryCB;
    private JButton igvDirectoryButton;
    private JLabel igvDirectoryField;
    private JLabel label21;
    private JPanel tooltipOptionsPanel;
    private JLabel label24;
    private JLabel label25;
    private JLabel label26;
    private JTextField toolTipInitialDelayField;
    private JTextField tooltipReshowDelayField;
    private JTextField tooltipDismissDelayField;
    private JCheckBox antialiasingCB;
    private JLabel label5;
    private JTextField blatURLField;
    private JPanel vSpacer8;
    private JPanel vSpacer9;
    private JPanel vSpacer10;
    private JScrollPane panel36;
    private JPanel cramPanel;
    private JPanel panel28;
    private JPanel panel37;
    private JLabel label28;
    private JTextField cramCacheSizeField;
    private JPanel panel38;
    private JLabel label29;
    private JTextField cramCacheDirectoryField;
    private JButton cramCacheDirectoryButton;
    private JCheckBox cramCacheReferenceCB;
    private ButtonPanel okCancelButtonPanel;
    JButton okButton;
    private JButton cancelButton;
    // End of variables declaration//GEN-END:variables

    public boolean isCanceled() {
        return canceled;
    }


}
