package org.regenstrief.linkage.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.regenstrief.linkage.MatchResult;
import org.regenstrief.linkage.db.SavedResultDBConnection;
import org.regenstrief.linkage.matchresult.DBMatchResultStore;
import org.regenstrief.linkage.matchresult.MatchResultStore;

public class MatchResultReviewPagerPanel extends JPanel implements ActionListener, WindowListener{

	private static final long serialVersionUID = 1L;
	public static int VIEWS_PER_PAGE = 5;
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private int view_index;
	private List<MatchResultReviewPanel> rpanels;
	private MatchResultStore mrs;
	private DateFormat ui_datefmt;
	
	private JButton next_page, prev_page, goto_mr, goto_first_unreviewed, open, save;
	private JTextField row, first_unreviewed, total;
	
	private Hashtable<Integer,MatchResult> reviewed_match_results;
	
	private List<String> order;
	private Set<String> no_display;
	
	private Connection db;
	
	private static String db_file_path;
	private static PrintStream review_log;
	
	public MatchResultReviewPagerPanel(){
		super();
		mrs = null;
		ui_datefmt = new SimpleDateFormat(DATE_FORMAT);
		rpanels = new ArrayList<MatchResultReviewPanel>();
		view_index = 0;
		reviewed_match_results = new Hashtable<Integer,MatchResult>();
		initGUI();
	}
	
	public void setOrder(List<String> order){
		this.order = order;
		Iterator<MatchResultReviewPanel> it = rpanels.iterator();
		while(it.hasNext()){
			MatchResultReviewPanel mrrp = it.next();
			mrrp.setOrder(this.order);
		}
	}
	
	public int getViewIndex(){
		return view_index;
	}
	
	public void setNonDisplay(Set<String> fields){
		no_display = fields;
		Iterator<MatchResultReviewPanel> it = rpanels.iterator();
		while(it.hasNext()){
			MatchResultReviewPanel mrrp = it.next();
			mrrp.setNonDisplayFields(no_display);
		}
	}
	
	private Date chooseDate(List<Date> options){
		// Date object toStrings can be ambiguous, so create more precise string
		Iterator<Date> it = options.iterator();
		Hashtable<String,Date> option_dates = new Hashtable<String,Date>();
		
		while(it.hasNext()){
			Date d = it.next();
			String str = ui_datefmt.format(d);
			option_dates.put(str, d);
		}
		
		Object[] o = option_dates.keySet().toArray();
		String choice = (String)JOptionPane.showInputDialog(
                this,
                "Multiple runs exist in review database.\nWhich run to open?",
                "Select date",
                JOptionPane.PLAIN_MESSAGE,
                null,
                o,
                o[0]);
		if(choice == null){
			return null;
		} else {
			return option_dates.get(choice);
		}
	}
	
	private void initGUI(){
		this.setLayout(new BorderLayout());
		
		
		// add top section
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
		open = new JButton("Open File");
		open.addActionListener(this);
		save = new JButton("Save Changes");
		save.addActionListener(this);
		JLabel first = new JLabel("First Unreviewed Row:");
		first_unreviewed = new JTextField(6);
		first_unreviewed.setEditable(false);
		JLabel total_label = new JLabel("Total Rows:");
		total = new JTextField(6);
		total.setEditable(false);
		
		top.add(Box.createRigidArea(new Dimension(5,0)));
		top.add(open);
		top.add(Box.createRigidArea(new Dimension(5,0)));
		top.add(save);
		top.add(Box.createRigidArea(new Dimension(5,0)));
		top.add(first);
		top.add(Box.createRigidArea(new Dimension(5,0)));
		top.add(first_unreviewed);
		top.add(Box.createRigidArea(new Dimension(5,0)));
		top.add(total_label);
		top.add(Box.createRigidArea(new Dimension(5,0)));
		top.add(total);
		top.add(Box.createHorizontalGlue());
		
		JPanel ttop = new JPanel();
		ttop.setLayout(new BorderLayout());
		ttop.add(top, BorderLayout.LINE_START);
		this.add(ttop, BorderLayout.PAGE_START);
		
		// create review panels
		MatchResultReviewPanel mrrp;
		JPanel middle = new JPanel();
		MatchResultReviewKeyboardAccelerator.INSTANCE.setReviewPanelList(rpanels);
		middle.setLayout(new BoxLayout(middle, BoxLayout.PAGE_AXIS));
		for(int i = 0; i < VIEWS_PER_PAGE; i++){
			String[] dummy_header = new String[8];
			for(int j = 0; j < dummy_header.length; j++){
				dummy_header[j] = "field";
			}
			mrrp = new MatchResultReviewPanel(dummy_header);
			rpanels.add(mrrp);
			middle.add(mrrp);
			middle.add(new JSeparator(JSeparator.HORIZONTAL));
		}
		JScrollPane jsp = new JScrollPane(middle);
		//this.add(middle, BorderLayout.CENTER);
		this.add(jsp, BorderLayout.CENTER);
		
		// create paging buttons
		JPanel bottom = new JPanel();
		prev_page = new JButton("Previous");
		prev_page.addActionListener(this);
		next_page = new JButton("Next");
		next_page.addActionListener(this);
		goto_mr = new JButton("Goto Row");
		goto_mr.addActionListener(this);
		row = new JTextField(4);
		goto_first_unreviewed = new JButton("Goto Unreviewed");
		goto_first_unreviewed.addActionListener(this);
		bottom.add(prev_page);
		bottom.add(Box.createRigidArea(new Dimension(5,0)));
		bottom.add(next_page);
		bottom.add(Box.createRigidArea(new Dimension(5,0)));
		bottom.add(goto_mr);
		bottom.add(Box.createRigidArea(new Dimension(5,0)));
		bottom.add(row);
		bottom.add(Box.createRigidArea(new Dimension(5,0)));
		bottom.add(goto_first_unreviewed);
		
		this.add(bottom, BorderLayout.PAGE_END);
		MatchResultReviewKeyboardAccelerator.INSTANCE.setPagerPanel(this);
	}
	
	
	public static void main(String[] args){
		JFrame window = new JFrame();
		window.setSize(1200, 800);
		MatchResultReviewPagerPanel mrrpp = new MatchResultReviewPagerPanel();
		window.add(mrrpp);
		window.setVisible(true);
		
		// parse arguments
		if(args.length >= 1){
			String arg_order = args[0];
			List<String> order = parseOrder(arg_order);
			mrrpp.setOrder(order);
		}
		if(args.length >= 2){
			String arg_display = args[1];
			Set<String> fields = parseNoDisplay(arg_display);
			mrrpp.setNonDisplay(fields);
		}
		
		// close database on exit, so implement window listener to close connection on exit
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.addWindowListener(mrrpp);
	}
	
	private static List<String> parseOrder(String order){
		String[] fields = order.split(",");
		List<String> ret = new ArrayList<String>();
		for(int i = 0; i < fields.length; i++){
			ret.add(fields[i]);
		}
		return ret;
	}
	
	private static Set<String> parseNoDisplay(String fields){
		String[] f = fields.split(",");
		Set<String> ret = new HashSet<String>();
		for(int i = 0; i < f.length; i++){
			ret.add(f[i]);
		}
		return ret;
	}
	
	public MatchResultStore getMatchResultStore(){
		return mrs;
	}
	
	public void setMatchResultStore(MatchResultStore mrs){
		this.mrs = mrs;
	}
	
	public void updateView(int index){
		for (MatchResultReviewPanel rpanel : rpanels) {
			rpanel.clearFocus();
		}
		// get VIEWS_PER_PAGE MatchResults out of MatchResult store beginning at given index
		// set GUI elements to show new MatchResults
		view_index = index;
		if(index < 0){
			view_index = 0;
		}
		int size = mrs.getSize();
		if(index > size - VIEWS_PER_PAGE){
			view_index = size - VIEWS_PER_PAGE;
			if(view_index < 0){
				view_index = 0;
			}
		}
		for(int i = 0; i < rpanels.size(); i++){
			MatchResultReviewPanel mrrp = rpanels.get(i);
			MatchResult mr;
			Integer key = Integer.valueOf(view_index + i);
			mr = reviewed_match_results.get(key);
			if(mr == null){
				mr = mrs.getMatchResult(view_index + i);
			}
			if(mr != null){
				mrrp.setMatchResult(mr);
				mrrp.setRow(view_index + i);
				
				reviewed_match_results.put(key, mr);
			}
		}
		MatchResultReviewKeyboardAccelerator.INSTANCE.setFocusIndex(0);
	}
	
	private void showPrevPage(){
		int new_index = view_index - VIEWS_PER_PAGE;
		if(new_index < 0){
			new_index = 0;
		}
		updateView(new_index);
	}
	
	private void showNextPage(){
		int new_index = view_index + VIEWS_PER_PAGE;
		int max = mrs.getSize();
		if(new_index > max){
			new_index = max;
		}
		updateView(new_index);
	}
	
	private void setTotal(){
		int size = mrs.getSize();
		total.setText(Integer.toString(size));
	}
	
	private void setFirstUnreviewed(){
		if(mrs instanceof DBMatchResultStore){
			DBMatchResultStore dmrs = (DBMatchResultStore)mrs;
			int first = dmrs.getMinUnknownID();
			first_unreviewed.setText(Integer.toString(first));
		}
	}
	
	private void saveChanges(){
		// iterate over MatchResult objects in reviewed_match_results and update or delete/add them to persist possible changes
		if(mrs != null){
			boolean use_db = mrs instanceof DBMatchResultStore;
			
			Enumeration<Integer> e = reviewed_match_results.keys();
			while(e.hasMoreElements()){
				Integer idKey = e.nextElement();
				MatchResult reviewed = reviewed_match_results.get(idKey);
				int id = idKey.intValue();
				if(use_db){
					DBMatchResultStore dmrs = (DBMatchResultStore)mrs;
					dmrs.updateMatchResult(id, reviewed.getNote(), reviewed.getMatch_status(), reviewed.getCertainty());
				} else {
					mrs.removeMatchResult(id);
					mrs.addMatchResult(reviewed, id);
				}
				
			}
			reviewed_match_results.clear();
			refreshCurrentMatchResults();
			closeReviewLog();
		}
	}
	
	private void refreshCurrentMatchResults() {
		final int size = rpanels.size();
		for (int i = 0; i < size; i++) {
			reviewed_match_results.put(Integer.valueOf(view_index + i), rpanels.get(i).mr);
		}
	}
	
	protected static void printReviewLog(final Object s) {
		try {
			if (review_log == null) {
				review_log = new PrintStream(new FileOutputStream(db_file_path + ".review.log", true), true);
			}
			review_log.println(s);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void closeReviewLog() {
		if (review_log == null) {
			return;
		}
		review_log.flush();
		review_log.close();
		review_log = null;
	}
	
	public void closeDBConnection(){
		if(mrs != null && mrs instanceof DBMatchResultStore){
			DBMatchResultStore dmrs = (DBMatchResultStore)mrs;
			dmrs.close();
			
			try{
				db.close();
			}
			catch(SQLException sqle){
				System.err.println(sqle.getMessage());
			}
		}
	}
	
	public boolean isOnFirstPage(){
		return view_index == 0;
	}
	
	public boolean isOnLastPage(){
		return mrs.getSize() < VIEWS_PER_PAGE || view_index >= mrs.getSize() - VIEWS_PER_PAGE;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if(ae.getSource() instanceof JButton){
			JButton source = (JButton)ae.getSource();
			if(source == open){
				// get MatchResultStore set and updateview
				JFileChooser jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				int retval = jfc.showOpenDialog(this);
				if(retval == JFileChooser.APPROVE_OPTION){
					File db_file = jfc.getSelectedFile();
					db_file_path = db_file.getAbsolutePath();
					db = SavedResultDBConnection.openDBResults(db_file);
					SavedResultDBConnection.validateMatchResultTables(db);
					if(db != null){
						mrs = new DBMatchResultStore(db);
						List<Date> dates = ((DBMatchResultStore)mrs).getDates();
						if(dates.size() > 1){
							Date choice = chooseDate(dates);
							((DBMatchResultStore)mrs).setDate(choice);
						} else {
							((DBMatchResultStore)mrs).setDate(dates.get(0));
						}
						updateView(view_index);
						setFirstUnreviewed();
						setTotal();
					}
				}
			} else if(source == next_page){
				showNextPage();
			} else if(source == prev_page){
				showPrevPage();
			} else if(source == save){
				final MatchResultReviewPanel focused = getFocusedPanel();
				saveChanges();
				setFirstUnreviewed();
				if (focused != null) {
					focused.setFocus();
				}
			} else if(source == goto_mr){
				try{
					updateView(Integer.parseInt(row.getText()));
				} catch(NumberFormatException nfe){
				}
			} else if(source == goto_first_unreviewed){
				try{
					updateView(Integer.parseInt(first_unreviewed.getText()));
				} catch(NumberFormatException nfe){
				}
			}
		}
	}
	
	private MatchResultReviewPanel getFocusedPanel() {
		for (final MatchResultReviewPanel panel : this.rpanels) {
			if (panel.focused) {
				return panel;
			}
		}
		return null;
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		if(db != null){
			int n = JOptionPane.showConfirmDialog(this,"Save review changes before closing?","Exitting program",JOptionPane.YES_NO_OPTION);
			if(n == JOptionPane.YES_OPTION){
				saveChanges();
			}
			closeDBConnection();
		}
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
	}
}
