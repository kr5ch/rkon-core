package net.kronos.rkon.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import net.kronos.rkon.core.ex.MalformedPacketException;

public class RconPacket {
	
	public static final int SERVERDATA_EXECCOMMAND = 2;
	public static final int SERVERDATA_AUTH = 3;
	
	private static final int READ_BUFFER_SIZE = 4096;
	
	private int requestId;
	private int type;
	private byte[] payload;
	
	private RconPacket(int requestId, int type, byte[] payload) {
		this.requestId = requestId;
		this.type = type;
		this.payload = payload;
	}
	
	public int getRequestId() {
		return requestId;
	}
	
	public int getType() {
		return type;
	}
	
	public byte[] getPayload() {
		return payload;
	}
	
	/**
	 * Get the payload with a specific charset
	 * 
	 * @param cs The charset ot use
	 * 
	 * @return The payload encoded with the charset 
	 */
	public String getPayloadAs(String cs) {
		return new String(getPayload(), Charset.forName(cs));
	}
	
	/**
	 * Send a Rcon packet and fetch the response
	 * 
	 * @param rcon Rcon instance
	 * @param type The packet type
	 * @param payload The payload (password, command, etc.)
	 * @return A RconPacket object containing the response
	 * 
	 * @throws IOException
	 * @throws MalformedPacketException 
	 */
	protected static RconPacket send(Rcon rcon, int type, byte[] payload) throws IOException {
		try {
			RconPacket.write(rcon.getSocket().getOutputStream(), rcon.getRequestId(), type, payload);
		}
		catch(SocketException se) {
			// Close the socket if something happens
			rcon.getSocket().close();
			
			// Rethrow the exception
			throw se;
		}
		
		return RconPacket.read(rcon.getSocket().getInputStream());
	}
	
	/**
	 * Write a rcon packet on an outputstream
	 * 
	 * @param out The OutputStream to write on
	 * @param requestId The request id
	 * @param type The packet type
	 * @param payload The payload
	 * 
	 * @throws IOException
	 */
	private static void write(OutputStream out, int requestId, int type, byte[] payload) throws IOException {
		int bodyLength = RconPacket.getBodyLength(payload.length);
		int packetLength = RconPacket.getPacketLength(bodyLength);
		
		ByteBuffer buffer = ByteBuffer.allocate(packetLength);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.putInt(bodyLength);
		buffer.putInt(requestId);
		buffer.putInt(type);
		buffer.put(payload);
		
		// Null bytes terminators
		buffer.put((byte)0);
		buffer.put((byte)0);
		
		// Woosh!
		out.write(buffer.array());
		out.flush();
	}
	
	/**
	 * Read an incoming rcon packet
	 * 
	 * @param in The InputStream to read on
	 * @return The read RconPacket
	 * 
	 * @throws IOException
	 * @throws MalformedPacketException 
	 */
	private static RconPacket read(InputStream in) throws IOException {
		byte[] receiveBuffer = new byte[READ_BUFFER_SIZE];
		
		// Read x bytes on input
		int bytesRead = in.read(receiveBuffer);
		
		try {
			ByteBuffer buffer = ByteBuffer.wrap(receiveBuffer, 0, bytesRead);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			
			int length = buffer.getInt();
			int requestId = buffer.getInt();
			int type = buffer.getInt();
			
			// Get only payload length, not packet body length
			byte[] payload = new byte[length - 4 - 4 - 2];
			
			buffer.get(payload);
			
			// Read the null bytes
			buffer.get(new byte[2]);
			
			return new RconPacket(requestId, type, payload);
		}
		catch(IndexOutOfBoundsException e) {
			throw new MalformedPacketException("Invalid packet length");
		}
		catch(BufferUnderflowException e) {
			throw new MalformedPacketException("Cannot read the whole packet");
		}
	}
	
	private static int getPacketLength(int bodyLength) {
		// 4 bytes for length + x bytes for body length
		return 4 + bodyLength;
	}
	
	private static int getBodyLength(int payloadLength) {
		// 4 bytes for requestId, 4 bytes for type, x bytes for payload, 2 bytes for two null bytes
		return 4 + 4 + payloadLength + 2;
	}

}
