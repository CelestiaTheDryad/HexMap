package bthomas.hexmap.permissions;

import java.util.regex.Pattern;

/**
 * Base class for permissions
 *
 * @author Brendan Thomas
 * @since 2019-03-14
 */
public abstract class PermissionBase {

	// "op" "hexmap.commands.addunit"
	public static final Pattern genericPermissionPattern = Pattern.compile("\\A([a-z]+\\.)*[a-z]+\\Z");
	// "op" "hexmap.command.addunit" "hexmap.*"
	public static final Pattern inputPermission = Pattern.compile("\\A([a-z]+\\.)*([a-z]+|\\*)\\Z");

	//could make each permission keep track of its key and parent
	//this would allow functionality like
	//ConnectionHandler.listAllPermissions()
	//but until that functionality is required, it would just be a memory waste
}
