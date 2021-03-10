package com.vinayemani.devsearch.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.vinayemani.devsearch.*;
import com.vinayemani.devsearch.data.UserData;
import com.vinayemani.devsearch.data.UserKey;

/**
 * A command line wrapper program over DevSearch library. Prompts for credentials and input/output file
 * locations.
 * 
 * @author Vinay E.
 *
 */
public class CLIWrapper {

	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		GitHubAPIClient client = null;
		while (true) {
			print("Choose one of the following options for authentication or to quit.");
			print("\t 1. Personal access token based auth.");
			print("\t 2. Username & password based auth.");
			print("\t 3. No auth.");
			print("\t 4. Quit");
			
			int choice = Integer.parseInt(reader.readLine().trim());
			if (choice < 0 || choice > 4) {
				print("Invalid choice. Try again");
				continue;
			} else if (choice == 4) {
				print("Quitting");
				reader.close();
				return;
			} else if (choice == 1) {
				print("Enter your personal access token");
				String token = reader.readLine().trim();
				try {
					client = new GitHubAPIClient(token);
					break;
				} catch (InvalidCredentialsException e) {
					print("Invalid PAT entered. Try again.");
					continue;
				}
			} else if (choice == 2) {
				print("Enter your username and password separated by space");
				String[] words = reader.readLine().split("[ ]+");
				if (words.length != 2) {
					print("Username/Password wrongly entered. Try again");
					continue;
				}
				
				try {
					client = new GitHubAPIClient(words[0], words[1]);
				} catch (InvalidCredentialsException e) {
					print("Invalid username/password. Try again.");
					continue;
				}
				break;
			} else if (choice == 3) {
				print("Creating a no. auth client");
				try {
				client = new GitHubAPIClient();
				} catch (InvalidCredentialsException e) {}
				break;
			}
		}
		
		if (client == null) {
			print("Unexpected error initializing the api client, quitting.");
			reader.close();
			return;
		}
		
		while (true) {
			print("Choose one of the following options for input/output file locations");
			print("\t 1. stdin , stdout");
			print("\t 2. inputFilePath, outputFilePath");
			print("\t 3. Quit");
			
			int choice = Integer.parseInt(reader.readLine().trim());
			if (choice < 0 || choice > 3) {
				print("Invalid choice. Try again");
				continue;
			} else if (choice == 3) {
				print("Quitting");
				reader.close();
				return;
			} else if (choice == 1) {
				print("Enter each user details one per line, separated by commas, e.g.");
				print("firstname,lastname,location");
				print("Currently spaces/commas in names/locations are not supported.");
				runInteractiveMode(client, reader);
				reader.close();
				return;
			} else {
				print("Enter input file and output file locations separated by space");
				String[] paths = reader.readLine().split("[ ]+");
				try {
					client.searchForUsers(paths[0], paths[1]);
				} catch (BadInputFileException e) {
					print("Bad input detected in " + paths[0]);
				}
				reader.close();
				return;
			}
		}
	}
	
	private static void runInteractiveMode(GitHubAPIClient client, BufferedReader reader) throws IOException {
		List<UserKey> userKeys = new ArrayList<>();
		while (true) {
			String line = reader.readLine();
			if (line == null || line.trim().isEmpty()) {
				break;
			}
			
			String[] parts = line.split(",");
			userKeys.add(new UserKey(parts[0], parts[1], parts[2]));
		}
		reader.close();
		
		for (UserData data : client.searchForUsers(userKeys).values()) {
			print(new JSONObject(data).toString(2));
		}
	}
	
	private static void print(String s) {
		System.out.println(s);
	}
}
