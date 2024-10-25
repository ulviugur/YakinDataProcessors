package com.langpack.datachannel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.langpack.common.ConfigReader;

public class DataChannelFactory {
	static final String CHANNEL_TYPE_MYSQL = "MYSQL";
	static final String CHANNEL_TYPE_ORACLE = "ORACLE";
	static final String CHANNEL_TYPE_ACCESS = "ACCESS";
	static final String CHANNEL_TYPE_FILE = "FILE";
	static final String CHANNEL_TYPE_XL = "XL";

	static TreeMap<String, DataChannel> channels = new TreeMap<>();
	static ConfigReader cfg = null;

	public static void initialize(String cfgFileName) {
		cfg = new ConfigReader(cfgFileName);
		try {
			cfg.initialize(cfgFileName);
			loadChannels();
		} catch (UnknownDataChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static final Logger log4j = LogManager.getLogger("DataChannelFactory");

	// No parameters will be fed to the channel, they are read from ConfigReader
	public static DataChannel addChannel(String channelKey)
			throws DataChannelParameterMissingException, UnknownDataChannelException {
		DataChannel instance = null;
		boolean connected = false;
		if (channelKey == null) {
			throw new DataChannelParameterMissingException("Channel Key is not found for channel : " + channelKey);
		} else {
			String channelType = cfg.getValue(channelKey + ".Type");
			if (channelType == null) {
				throw new DataChannelParameterMissingException("Channel Type is not found for channel : " + channelKey);
			} else {
				switch (channelType.toUpperCase()) {
				case CHANNEL_TYPE_MYSQL:
					instance = new MySQLDataChannel(cfg, CHANNEL_TYPE_MYSQL, channelKey);

					try {
						connected = instance.initialize();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (connected) {
						channels.put(channelKey, instance);
					}
					break;
				case CHANNEL_TYPE_ORACLE:
					instance = new OracleDataChannel(cfg, CHANNEL_TYPE_ORACLE, channelKey);
					try {
						connected = instance.initialize();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (connected) {
						channels.put(channelKey, instance);
					}
					break;
				case CHANNEL_TYPE_ACCESS:
					instance = new AccessDBChannel(cfg, CHANNEL_TYPE_ACCESS, channelKey);
					try {
						connected = instance.initialize();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (connected) {
						channels.put(channelKey, instance);
					}
					break;
				case CHANNEL_TYPE_FILE:
					instance = new FileChannel(cfg, CHANNEL_TYPE_FILE, channelKey);
					try {
						connected = instance.initialize();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (connected) {
						channels.put(channelKey, instance);
					}
					break;
				case CHANNEL_TYPE_XL:
					instance = new XLChannel(cfg, CHANNEL_TYPE_XL, channelKey);
					try {
						connected = instance.initialize();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (connected) {
						channels.put(channelKey, instance);
					}
					break;
				default:
					break;
				}
			}
		}
		return instance;
	}

	public static void loadChannels() throws UnknownDataChannelException {
		int channelCount = 0;
		String channelNameParameter = null;
		String channelIdName = null;
		boolean cont = true;
		do {
			channelCount++; // start the channel count from 1
			channelIdName = String.format("%s%s", cfg.getValue("Prefix"), channelCount);
			channelNameParameter = String.format("%s.%s", channelIdName, "Name");

			log4j.info(String.format("Loading channel : %s", channelIdName));

			if ((cfg.getValue(channelNameParameter)) != null) {
				try {
					addChannel(channelIdName);
				} catch (DataChannelParameterMissingException | UnknownDataChannelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				log4j.info(String.format("Channel %s not found, skipping !", channelIdName));
				cont = false;
			}

		} while (cont);
		String tmp = listDataChanneltoString();
		log4j.info(tmp);
	}

	public static DataChannel getChannelById(String channelId) throws UnknownDataChannelException {
		DataChannel retval = channels.get(channelId);
		return retval;
	}

	public static DataChannel getChannelByName(String tmpName) {
		DataChannel retval = null;

		int count = 1;
		for (String key : channels.keySet()) {
			DataChannel item = channels.get(key);
			String name = item.getName();
			if (name.equals(tmpName)) {
				retval = item;
				break;
			}
		}
		return retval;
	}

	public static String listDataChanneltoString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");

		int count = 1;
		for (String tmpName : channels.keySet()) {
			String key = cfg.getValue("Prefix") + count;
			DataChannel item = channels.get(key);
			String desc = item.toString();
			sb.append(String.format("[ (%s) = %s]\n", count, desc));
			count++;
		}
		sb.append("}");

		return sb.toString();
	}

	public static void closeChannels() {
		for (String tmpName : channels.keySet()) {
			DataChannel tmpChannel = channels.get(tmpName);
			tmpChannel.closeChannel();
		}
	}

}