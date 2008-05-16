package org.regenstrief.linkage.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.regenstrief.linkage.analysis.EMAnalyzer;
import org.regenstrief.linkage.analysis.PairDataSourceAnalysis;
import org.regenstrief.linkage.analysis.RandomSampleAnalyzer;
import org.regenstrief.linkage.analysis.VectorTable;
import org.regenstrief.linkage.io.OrderedDataSourceReader;
import org.regenstrief.linkage.io.ReaderProvider;
import org.regenstrief.linkage.util.MatchingConfig;
import org.regenstrief.linkage.util.RecMatchConfig;

/**
 * Class displays different analysis options available in the record linkaeg GUI
 * 
 * @author jegg
 *
 */

public class AnalysisPanel extends JPanel implements ActionListener{
	RecMatchConfig rm_conf;
	
	private JButton the_button, vector_button;
	
	public AnalysisPanel(RecMatchConfig rmc){
		super();
		rm_conf = rmc;
		createAnalysisPanel();
	}
	
	public void setRecMatchConfig(RecMatchConfig rmc){
		rm_conf = rmc;
	}
	
	private void createAnalysisPanel(){
		//this.setLayout(new BorderLayout());
		the_button = new JButton("Perform EM Analysis");
		this.add(the_button);
		the_button.addActionListener(this);
		
		vector_button = new JButton("View score tables");
		this.add(vector_button);
		vector_button.addActionListener(this);
	}
	
	private void runEMAnalysis(){
		ReaderProvider rp = new ReaderProvider();
		List<MatchingConfig> mcs = rm_conf.getMatchingConfigs();
		Iterator<MatchingConfig> it = mcs.iterator();
		while(it.hasNext()){
			MatchingConfig mc = it.next();
			
			OrderedDataSourceReader odsr1 = rp.getReader(rm_conf.getLinkDataSource1(), mc);
			OrderedDataSourceReader odsr2 = rp.getReader(rm_conf.getLinkDataSource2(), mc);
			if(odsr1 != null && odsr2 != null){
				// analyze with EM
				org.regenstrief.linkage.io.FormPairs fp2 = new org.regenstrief.linkage.io.FormPairs(odsr1, odsr2, mc, rm_conf.getLinkDataSource1().getTypeTable());
				/*
				 * Using two analyzer at a time in the PairDataSourceAnalysis. The order when adding the analyzer to
				 * PairDataSourceAnalysis will affect the end results. For example in the following code fragment,
				 * RandomSampleAnalyzer will be run first followed by the EMAnalyzer. But this will be depend on
				 * current Java's ArrayList implementation, if they add new element add the end of the list, then
				 * this will work fine.
				 * 
				 * In the following code, RandomSampleAnalyzer and EMAnalyzer will work independent each other.
				 * RandomSampleAnalyzer will generate the u value and save it in MatchingConfigRow object, while
				 * EMAnalyzer will check MatchingConfig to find out whether the blocking run use random sampling
				 * (where u value that will be used is the one generated by RandomSampleAnalyzer) or not using 
				 * random sampling (where u value will be the default value).
				 * 
				 * I don't think we need to instantiate the RandomSampleAnalyzer here if the user doesn't want to
				 * use random sampling :D
				 */
				PairDataSourceAnalysis pdsa = new PairDataSourceAnalysis(fp2);
				RandomSampleAnalyzer rsa = new RandomSampleAnalyzer(rm_conf.getLinkDataSource1(), rm_conf.getLinkDataSource2(), mc);
				EMAnalyzer ema = new EMAnalyzer(rm_conf.getLinkDataSource1(), rm_conf.getLinkDataSource2(), mc);
				// EMRandomSampleSourceAnalysis emrsa = new EMRandomSampleSourceAnalysis(fp2, ema, rsa, mc);
				pdsa.addAnalyzer(rsa);
				pdsa.addAnalyzer(ema);
				LoggingFrame lf = new LoggingFrame(ema, mc.getName());
				LoggingFrame lf2 = new LoggingFrame(rsa, mc.getName());
				// emrsa.analyzeData();
				//try{
					pdsa.analyzeData();
				//}
				//catch(IOException ioe){
				//	JOptionPane.showMessageDialog(this, "IOException: " + ioe.getMessage() + " while running analysis","Analysis Error", JOptionPane.ERROR_MESSAGE);
				//}
			}
			odsr1.close();
			odsr2.close();
		}
	}
	
	private void displayVectorTables(){
		Iterator<MatchingConfig> it = rm_conf.getMatchingConfigs().iterator();
		while(it.hasNext()){
			MatchingConfig mc = it.next();
			VectorTable vt = new VectorTable(mc);
			TextDisplayFrame tdf = new TextDisplayFrame(mc.getName(), vt.toString());
		}
	}
	
	public void actionPerformed(ActionEvent ae){
		if(ae.getSource() == the_button){
			runEMAnalysis();
		} else if(ae.getSource() == vector_button){
			displayVectorTables();
		}
	}
}