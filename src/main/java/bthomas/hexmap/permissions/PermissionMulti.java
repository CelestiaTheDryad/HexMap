package bthomas.hexmap.permissions;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * This class is used to manage the permissions in a Hexmap server
 * Permissions are set up by creating a tree of these managers
 *
 * @author Brendan Thomas
 * @since 2019-03-14
 */
public class PermissionMulti extends PermissionBase {

	// "apple" "teleport" "addunit"
	private static final Pattern permissionName = Pattern.compile("\\A[a-z]+\\Z");

	private HashMap<String, PermissionMulti> subMultis = new HashMap<>();
	private HashMap<String, PermissionSingle> permissions = new HashMap<>();

	/**
	 * Gets the sub-manager for a given permissions tree node, creating it if necessary
	 *
	 * @param permission The key for the subnode to find
	 * @return The requested sub-manager, or null if an invalid permission name was given
	 */
	public PermissionMulti getSubMultiOrCreate(String permission) {
		//reject invalid permission names
		if(!permissionName.matcher(permission).matches()) {
			return null;
		}

		PermissionMulti res = subMultis.get(permission);

		//create a new manager if needed
		if(res == null) {
			res = new PermissionMulti();
			subMultis.put(permission, res);
		}

		return res;
	}

	/**
	 * Gets a sub-manager by name if it exists. Used to look up registered permissions
	 *
	 * @param permission The name of the submanager to look up, eg "commands"
	 * @return The sub-manager if it exists, null if it does not
	 */
	public PermissionMulti getSubManagerOrFail(String permission) {
		return subMultis.get(permission);
	}

	/**
	 * Gets a permission from this manager if it exists. Used to look up registered permissions
	 *
	 * @param permission the name of the permission to look up, eg "roll"
	 * @return The permission if it exists, null if it does not
	 */
	public PermissionSingle getPermissionOrFail(String permission) {
		return permissions.get(permission);
	}

	/**
	 * Registers a permission in this manager
	 *
	 * @param permission The permission key to register
	 * @return True if the permission was correctly registered, false otherwise
	 */
	public boolean registerPermission(String permission) {
		//reject duplicate permissions
		if(permissions.get(permission) != null) {
			return false;
		}

		permissions.put(permission, new PermissionSingle());
		return true;
	}
}
