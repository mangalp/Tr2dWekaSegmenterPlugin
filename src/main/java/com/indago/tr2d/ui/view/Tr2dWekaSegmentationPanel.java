/**
 *
 */
package com.indago.tr2d.ui.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.indago.io.ProjectFile;
import com.indago.tr2d.ui.model.Tr2dWekaSegmentationModel;
import com.indago.tr2d.ui.util.JDoubleListTextPane;
import com.indago.tr2d.ui.util.UniversalFileChooser;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;
import net.miginfocom.swing.MigLayout;
import weka.gui.ExtensionFileFilter;

/**
 * @author jug
 */
public class Tr2dWekaSegmentationPanel extends JPanel implements ActionListener, ListSelectionListener {

	private static final long serialVersionUID = 9192569315077150275L;

	private final Tr2dWekaSegmentationModel model;

	private final JList< String > listClassifiers;
	private final JButton bAdd = new JButton( "+" );
	private final JButton bRemove = new JButton( "-" );

	private JLabel lblThresholds;
	private JDoubleListTextPane txtThresholds;

	private JButton bStartSegmentation;

	public Tr2dWekaSegmentationPanel( final Tr2dWekaSegmentationModel model ) {
		super( new BorderLayout() );
		this.model = model;
		listClassifiers = new JList<>( model.getListModel() );
		buildGui();
	}

	/**
	 * Builds the GUI of this panel.
	 */
	private void buildGui() {

//		lblClassifier = new JLabel( "classifier: " );
//		txtClassifierPath = new JTextPane();
//		String fn = "";
//		try {
//			fn = model.getClassifierFilenames().get( 0 );
//		} catch ( final IndexOutOfBoundsException e ) {}
//		txtClassifierPath.setText( fn );
//		bOpenClassifier = new JButton( "pick classifier" );
//		bOpenClassifier.addActionListener( this );

		final MigLayout layout = new MigLayout( "", "[grow]", "" );
		final JPanel controls = new JPanel( layout );

		final JPanel list = new JPanel( new BorderLayout() );
		listClassifiers.addListSelectionListener( this );
		list.add( listClassifiers, BorderLayout.CENTER );
		JPanel helper = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
		bAdd.addActionListener( this );
		bRemove.addActionListener( this );
		helper.add( bAdd );
		helper.add( bRemove );
		list.add( helper, BorderLayout.SOUTH );
		controls.add( list, "growx, wrap" );

		helper = new JPanel( new BorderLayout() );
		lblThresholds = new JLabel( "thresholds: " );
		txtThresholds = new JDoubleListTextPane();
		helper.add( lblThresholds, BorderLayout.WEST );
		helper.add( txtThresholds, BorderLayout.CENTER );
		controls.add( helper, "growx, growy, wrap" );

		bStartSegmentation = new JButton( "segment" );
		bStartSegmentation.addActionListener( this );
		controls.add( bStartSegmentation, "growx, wrap" );

		final BdvHandlePanel bdv = new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
				.options()
				.is2D()
				.inputTriggerConfig( model.getModel().getModel().getDefaultInputTriggerConfig() ) );
		model.bdvSetHandlePanel( bdv );

		this.add( controls, BorderLayout.WEST );
		this.add( model.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( bAdd ) ) {
			actionAddClassifierFile();
		} else if ( e.getSource().equals( bRemove ) ) {
			actionRemoveClassifierData();
		} else if ( e.getSource().equals( bStartSegmentation ) ) {
			actionStartSegmentaion();
		}
	}

	/**
	 * Opens FileChooser to select a classifier later used for Weka
	 * segmentation.
	 */
	private void actionAddClassifierFile() {
		final ExtensionFileFilter eff =
				new ExtensionFileFilter( new String[] { "model", "MODEL" }, "weka-model-file" );
		final File file = UniversalFileChooser.showLoadFileChooser(
				this,
				"",
				"Classifier to be loaded...",
				eff );
		if ( file != null ) {
			model.importClassifier( file );
			listClassifiers.setSelectedIndex( listClassifiers.getModel().getSize() - 1 );
		}
	}

	/**
	 *
	 */
	private void actionRemoveClassifierData() {
		if ( listClassifiers.getSelectedIndices().length > 0 ) {
			int removedSoFar = 0;
			for ( final int idx : listClassifiers.getSelectedIndices() ) {
				listClassifiers.remove( idx - removedSoFar );
				removedSoFar++;
				model.removeClassifierAndData( idx - removedSoFar );
			}
		}
		if ( listClassifiers.getModel().getSize() > 0 ) {
			listClassifiers.setSelectedIndex( 0 );
		} else {
			model.bdvRemoveAll();
		}
	}

	/**
	 * Start segmentation procedure.
	 */
	private void actionStartSegmentaion() {

		if ( listClassifiers.getSelectedIndex() != -1 ) {
			model.setListThresholds( listClassifiers.getSelectedIndex(), txtThresholds.getList() );
		}
		model.segment();

		int i = 0;
		for( final ProjectFile classifier : model.getClassifiers() ) {
			final RandomAccessibleInterval< IntType > seghyps = model.getSumImages().get( i );
			model.bdvAdd( seghyps, "result of " + classifier.getFilename() );
    		i++;
		}
	}

	/**
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	@Override
	public void valueChanged( final ListSelectionEvent e ) {
		if ( e.getValueIsAdjusting() == false ) {

			model.bdvRemoveAll();
			if ( listClassifiers.getSelectedIndices().length > 0 ) {
				for ( final int idx : listClassifiers.getSelectedIndices() ) {
					if ( listClassifiers.getSelectedIndices().length > 1 ) {
						txtThresholds.setEnabled( false );
					} else {
						txtThresholds.setList( model.getListThresholds( idx ) );
						txtThresholds.setEnabled( true );
					}
					if ( model.getSumImages().size() > idx ) {
						model.bdvAdd(
								model.getSumImages().get( idx ),
								listClassifiers.getModel().getElementAt( idx ) );
					}
				}
			} else {
				model.bdvRemoveAll();
			}
		}
	}
}
