package com.getman;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.event.*;

public class BeatBox {
	static String[] instrumentNames = { "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
			"Hand Clap", "High Tom", "High Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibra Slap",
			"Low-mid Tom", "High Agoga", "Open Hi Conga", };
	static int[] instruments = { 35, 52, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63 };
	static int numberOfInstruments = instruments.length;
	static int numberOfBeats = 16;
	static int numberOfCheckBoxes = numberOfInstruments * numberOfInstruments;

	public static MidiEvent makeEvent(int command, int channel, int one, int two, int tick) {
		MidiEvent event = null;
		try {
			ShortMessage a = new ShortMessage();
			a.setMessage(command, channel, one, two);
			event = new MidiEvent(a, tick);
		} catch (Exception e) {
		}

		return event;
	}

	JPanel checkBoxPanel;
	ArrayList<JCheckBox> checkBoxList;
	Sequencer sequencer;
	Sequence sequence;
	Track track;
	JFrame frame;
	JTextField userMessage;
	JList<String> incomingList;
	String userName;
	int nextNum;
	ObjectInputStream is;
	ObjectOutputStream os;
	// Vector<String> chatMessages;
	DefaultListModel<String> chatMessages;
	HashMap<String, boolean[]> sequencesFromChat;

	public static void main(String[] args) {
		BeatBox beatBox = new BeatBox();
		beatBox.start();
	}

	BeatBox() {
		chatMessages = new DefaultListModel<String>();
		sequencesFromChat = new HashMap<String, boolean[]>();
	}

	void start() {
		buildGUI();
		setUpMidi();
		while (userName == null || userName.equals("")) {
			userName = popupUsernameDialog();
		}
		setUpNetworking();
	}

	String popupUsernameDialog() {

		String username = (String) JOptionPane.showInputDialog(frame,
				"The username was not specified at launch.\n"
						+ "Please enter your username that will appear in the chat below: ",
				"Choose username", JOptionPane.INFORMATION_MESSAGE);
		return username;
	}

	private void buildGUI() {
		frame = new JFrame("Cyber BeatBox");
		JPanel background = setUpAndGetFrameBackground();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().add(background);

		frame.setBounds(50, 50, 500, 500);
		frame.pack();

		frame.setVisible(true);

	}

	private void setUpCheckBoxPanelAndList() {
		checkBoxList = new ArrayList<JCheckBox>();

		// new GridLayout(int rows, int columns, int horizontalGap, int verticalGap);

		GridLayout grid = new GridLayout(numberOfInstruments, numberOfInstruments, 2, 1);

		checkBoxPanel = new JPanel(grid);

		for (int i = 0; i < numberOfCheckBoxes; i++) {
			JCheckBox checkBox = new JCheckBox();
			checkBox.setSelected(false);

			checkBoxPanel.add(checkBox);
			checkBoxList.add(checkBox);
		}
	}

	private JPanel setUpAndGetFrameBackground() {
		Box buttonBox = setUpAndGetButtonBox();
		Box nameBox = setUpAndGetInstrumentNameBox();
		setUpCheckBoxPanelAndList();

		BorderLayout borderLayout = new BorderLayout();
		JPanel background = new JPanel(borderLayout);

		background.add(checkBoxPanel, BorderLayout.CENTER);
		background.add(buttonBox, BorderLayout.EAST);
		background.add(nameBox, BorderLayout.WEST);

		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		return background;
	}

	private Box setUpAndGetInstrumentNameBox() {
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		nameBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		for (String name : instrumentNames) {
			addComponentWithBottomMargin(nameBox, new JLabel(name), 10);
		}
		return nameBox;
	}

	private Box setUpAndGetButtonBox() {
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		buttonBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));

		JButton startButton = new JButton("Start");
		JButton stopButton = new JButton("Stop");
		JButton tempoUpButton = new JButton("Tempo up");
		JButton tempoDownButton = new JButton("Tempo down");
		JButton clearButton = new JButton("Clear");
		JButton saveButton = new JButton("Save");
		JButton loadButton = new JButton("Load");
		userMessage = new JTextField();
		JPanel chatPanel = setUpAndGetChatPanel(new JPanel(new BorderLayout()));

		startButton.addActionListener(new StartListener());
		stopButton.addActionListener(new StopListener());
		tempoUpButton.addActionListener(new TempoUpListener());
		tempoDownButton.addActionListener(new TempoDownListener());
		clearButton.addActionListener(new ClearListener());
		saveButton.addActionListener(new FileSaveListener());
		loadButton.addActionListener(new FileReadListener());

		addComponentWithBottomMargin(buttonBox, startButton, 10);
		addComponentWithBottomMargin(buttonBox, stopButton, 10);
		addComponentWithBottomMargin(buttonBox, tempoUpButton, 10);
		addComponentWithBottomMargin(buttonBox, tempoDownButton, 10);
		addComponentWithBottomMargin(buttonBox, clearButton, 10);
		addComponentWithBottomMargin(buttonBox, saveButton, 10);
		addComponentWithBottomMargin(buttonBox, loadButton, 10);
		addComponentWithBottomMargin(buttonBox, chatPanel, 10);

		return buttonBox;
	}

	private JPanel setUpAndGetChatPanel(JPanel chatPanel) {
		JPanel messagePanel = new JPanel(new BorderLayout());
		JScrollPane chat = setUpAndGetScrollableChat();

		JButton sendButton = new JButton("Send");
		sendButton.addActionListener(new SendListener());

		messagePanel.add(BorderLayout.CENTER, userMessage);
		messagePanel.add(BorderLayout.EAST, sendButton);
		chatPanel.add(BorderLayout.SOUTH, messagePanel);
		chatPanel.add(BorderLayout.CENTER, chat);

		return chatPanel;
	}

	private JScrollPane setUpAndGetScrollableChat() {
		incomingList = new JList<String>(chatMessages);
		incomingList.addListSelectionListener(new MessageSelectedListener());
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane pane = new JScrollPane(incomingList);
		pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		return pane;
	}

	private void addComponentWithBottomMargin(Box box, Component component, int bottomMargin) {
		box.add(component);
		box.add(Box.createRigidArea(new Dimension(0, bottomMargin)));
	}

	private void clearInput() {
		for (JCheckBox checkBox : checkBoxList) {
			checkBox.setSelected(false);
		}
	}

	private void setCheckBoxesAccordingTo(boolean[] receivedStates) {
		for (int i = 0; i < receivedStates.length; i++) {
			JCheckBox currentCheckBox = (JCheckBox) checkBoxList.get(i);
			currentCheckBox.setSelected(receivedStates[i]);
		}
	}

	private boolean[] convertInputToArray(ArrayList<JCheckBox> checkBoxList) {
		boolean[] checkBoxState = new boolean[256];
		for (int i = 0; i < checkBoxList.size(); i++) {
			JCheckBox thisCheckBox = (JCheckBox) checkBoxList.get(i);
			if (thisCheckBox.isSelected())
				checkBoxState[i] = true;
		}
		return checkBoxState;

	}

	private void setUpNetworking() {
		try {
			Socket socket = new Socket("192.168.1.21", 4242);
			is = new ObjectInputStream(socket.getInputStream());
			os = new ObjectOutputStream(socket.getOutputStream());
			Thread messageReceiver = new Thread(new MessageListener());
			messageReceiver.start();
			chatMessages.addElement("Connected to server");
		} catch (Exception e) {
			chatMessages.addElement("Could not connect, Game is in offline mode");
			chatMessages.addElement(e.getMessage());
		}

	}

	private void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();

			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void startPlaying() {
		buildTrack();
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void buildTrack() {
		sequence.deleteTrack(track); // refresh track
		track = sequence.createTrack();
		buildPartsForTrack();
	}

	private void buildPartsForTrack() {
		int[] part = null;

		for (int j = 0; j < numberOfInstruments; j++) {
			part = new int[numberOfBeats];

			int key = instruments[j];

			for (int beat = 0; beat < numberOfBeats; beat++) {
				int currentCheckBoxNumber = beat + numberOfBeats * j;
				JCheckBox currentCheckBox = (JCheckBox) checkBoxList.get(currentCheckBoxNumber);
				if (currentCheckBox.isSelected())
					part[beat] = key;
				else
					part[beat] = 0;
			}

			addEventsToTrackFrom(part);
		}
		track.add(makeEvent(ShortMessage.PROGRAM_CHANGE, 9, 1, 0, 15)); // в конечном такта всегда должен быть ивент,
																		// чтобы
																		// трек зациклился полностью
	}

	private void addEventsToTrackFrom(int[] part) {
		for (int i = 0; i < part.length; i++) {
			int key = part[i];
			if (key != 0) {
				track.add(makeEvent(ShortMessage.NOTE_ON, 9, key, 100, i));
				track.add(makeEvent(ShortMessage.NOTE_OFF, 9, key, 100, i + 1));
			}
		}
		track.add(makeEvent(176, 1, 127, 0, 16));
	}

	boolean saveCurrentSequence() {
		boolean[] checkBoxState = convertInputToArray(checkBoxList);

		try {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.showOpenDialog(frame);
			FileOutputStream fs = new FileOutputStream(fileChooser.getSelectedFile());
			ObjectOutputStream os = new ObjectOutputStream(fs);
			os.writeObject(checkBoxState);
			os.close();
			return true;
		}catch(NullPointerException e) {
			
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		return false;
	}
	
	boolean userWantsToSaveCurrentSequence() {
		Object[] options = { "Proceed without saving", "Save current Beat" };
		int answer = JOptionPane.showOptionDialog(frame,
				"The current beat will dissapear after loading selected beat from chat.\nDo you want to save it?",
				"Saving beat", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
		
		return answer == 0 ? false : true;
	}

	class MessageSelectedListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			if (!event.getValueIsAdjusting()) {
				String selected = (String) incomingList.getSelectedValue();
				if (selected != null) {
					boolean[] selectedState = sequencesFromChat.get(selected);

					if (userWantsToSaveCurrentSequence()) {
						saveCurrentSequence();
					}
					
					setCheckBoxesAccordingTo(selectedState);
					sequencer.stop();
					startPlaying();
				}
			}
		}
	}

	class MessageListener implements Runnable {
		public void run() {
			String message = null;
			Object obj = null;
			boolean[] checkboxesState = null;
			try {
				while ((obj = is.readObject()) != null) {
					message = (String) obj;
					System.out.println("Received message from the server: " + message);
					checkboxesState = (boolean[]) is.readObject();
					sequencesFromChat.put(message, checkboxesState);
					chatMessages.addElement(message);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	class SendListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			boolean[] checkBoxState = convertInputToArray(checkBoxList);

			String message = userName + nextNum++ + ":" + userMessage.getText();

			try {
				os.writeObject(message);
				os.writeObject(checkBoxState);
			} catch (IOException e) {
				System.out.println("Could not send to server");
				e.printStackTrace();
			}

			userMessage.setText("");
		}
	}

	class StartListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			startPlaying();
		}
	}

	class StopListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			sequencer.stop();
		}
	}

	class TempoUpListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * 1.03));
		}
	}

	class TempoDownListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * 0.97));
		}
	}

	class ClearListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			clearInput();
		}
	}

	class FileSaveListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			saveCurrentSequence();
		}
	}

	class FileReadListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.showOpenDialog(frame);

				FileInputStream fs = new FileInputStream(fileChooser.getSelectedFile());
				ObjectInputStream is = new ObjectInputStream(fs);

				boolean[] receivedStates = (boolean[]) is.readObject();

				is.close();

				setCheckBoxesAccordingTo(receivedStates);
			} catch (NullPointerException ex) {

			} catch (ClassNotFoundException ex) {
				System.out.println("The class coresponding to serialized object was not found!");
			} catch (FileNotFoundException ex) {
				System.out.println("File not found!");
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
