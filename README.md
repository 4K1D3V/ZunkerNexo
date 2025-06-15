# ZunkerNexo


# ZunkerNexo Plugin Tester Guide

This guide provides step-by-step instructions to test the ZunkerNexo plugin (version 1.0-DEV) on a Minecraft Paper 1.21.5 server with Nexo 1.7.1. The plugin creates custom storage blocks with MySQL persistence, supporting various interactions and configurations. Follow the steps below to verify each feature, checking for expected behavior and noting any issues. Testing should be completed by June 15, 2025.

## Prerequisites
- **Server**: Paper 1.21.5 with `paper-1.21.5.jar` and `Nexo.jar` (version 1.7.1) in the `plugins` folder.
- **Plugin**: `ZunkerNexo-1.0-DEV.jar` in the `plugins` folder.
- **Java**: Temurin Java 21.
- **MySQL**: Running server with a database (e.g., `zunkernexo`) and valid credentials.
- **Permissions**: Admin/OP access for initial testing; non-OP account for permissions tests.
- **Tools**: Optional permissions plugin (e.g., LuckPerms) and Multiverse for edge cases.
- **Reporting**: Access to `latest.log` and a way to document issues (e.g., screenshots, notes).

## Setup Instructions
1. **Start Server**:
   - Run `java -jar paper-1.21.5.jar`, accept the EULA (`eula.txt`: set `eula=true`), and stop the server.
2. **Configure MySQL**:
   - Create a MySQL database: `CREATE DATABASE zunkernexo;`.
   - Edit `plugins/ZunkerNexo/config.yml`:
     ```yaml
     mysql:
       host: localhost
       port: 3306
       database: zunkernexo
       username: your_username
       password: your_password
       pool-size: 10
     storage-blocks:
       example_block:
         nexo-id: "example:nexo_block"  # Replace with valid ID from /nexo list
         menu-title: "&6Example Storage"
         rows: 5
     ```
   - Start the server to verify MySQL connection (check logs for “ZunkerNexo enabled successfully!”).
3. **Obtain Nexo Block**:
   - Join the server, run `/nexo list` to find valid `nexo-id` values.
   - Use `/nexo give <nexo-id>` to get the block matching `config.yml`’s `nexo-id`.
4. **Prepare Accounts**:
   - Ensure you have an OP account (`/op <your_username>`).
   - Create or use a non-OP account for permissions testing.

## Test Cases

### 1. Right-Click Interaction (Open Storage)
**Steps**:
1. Place a Nexo block configured in `config.yml`.
2. Right-click the block to open the storage menu.
3. Add items (e.g., 10 stacks of cobblestone) to the inventory, close it, and right-click again.

**Checks**:
- [ ] Menu opens with 5 rows (45 slots) and title “Example Storage”.
- [ ] Message appears: “Opened storage: Example Storage”.
- [ ] Items persist after closing and reopening the menu.
- [ ] In MySQL, check `nexo_storage` table’s `inventory` column contains Base64-encoded data.
- [ ] No errors in console or in-game.

**Notes**:
- If menu size or title is incorrect, note the actual values.
- Use a MySQL client (e.g., phpMyAdmin) to verify database entries.

### 2. Left-Click Interaction (Info Display)
**Steps**:
1. Left-click the placed Nexo block.
2. Right-click to open the menu, add/remove items, close, and left-click again.

**Checks**:
- [ ] Message appears: “Storage Info: Example Storage (X/45 slots used)” (X = non-empty slots).
- [ ] Item count updates correctly after adding/removing items.
- [ ] No menu opens on left-click.
- [ ] No errors in console or in-game.

**Notes**:
- Record the exact message and slot count for accuracy.
- Test with empty and partially filled inventories.

### 3. Block Breaking
**Steps**:
1. Place a Nexo block and add items to its inventory via right-click.
2. Break the block.

**Checks**:
- [ ] Stored items drop at the block’s location.
- [ ] Message appears: “Storage block broken, items dropped.”
- [ ] MySQL `nexo_storage` table entry for the block is removed.
- [ ] Nexo’s default block drops (if any) are present.
- [ ] No errors in console or in-game.

**Notes**:
- Count dropped items to ensure all are accounted for.
- Check MySQL before and after breaking (e.g., `SELECT * FROM nexo_storage;`).

### 4. Permissions
**Steps**:
1. De-op yourself (`/deop <your_username>`) or switch to a non-OP account.
2. Remove `zunkernexo.interact` permission (e.g., `/lp user <username> permission unset zunkernexo.interact` with LuckPerms).
3. Try left-clicking and right-clicking the Nexo block.
4. Re-add `zunkernexo.interact` permission and repeat clicks.
5. Test `/zunkernexo` commands with and without `zunkernexo.use`/`zunkernexo.reload` permissions.

**Checks**:
- [ ] Without `zunkernexo.interact`: Message: “You don’t have permission to interact with this storage block!”; no menu opens.
- [ ] With `zunkernexo.interact`: Left/right-click works normally.
- [ ] Commands respect `zunkernexo.use` (for `/zunkernexo`) and `zunkernexo.reload` (for `/zunkernexo reload`).
- [ ] No errors in console or in-game.

**Notes**:
- Note exact permission denial messages.
- If no permissions plugin is used, test with de-opped account only.

### 5. Commands
**Steps**:
1. Run `/zunkernexo`, `/zn`, and `/zunkernexo reload` as an OP.
2. Use tab completion for `/zunkernexo`.
3. De-op or remove `zunkernexo.use`, try `/zunkernexo` again.

**Checks**:
- [ ] `/zunkernexo`: Shows “ZunkerNexo - Version 1.0-DEV”.
- [ ] `/zunkernexo reload`: Message: “ZunkerNexo configuration reloaded!”; no errors.
- [ ] Tab completion suggests `reload` for users with `zunkernexo.reload`.
- [ ] Without `zunkernexo.use`: Message: “You don’t have permission to use this command!”.
- [ ] No errors in console or in-game.

**Notes**:
- Verify reload updates `config.yml` changes (e.g., change `menu-title`, reload, check menu).

### 6. MySQL Retry Mechanism
**Steps**:
1. Stop the MySQL server.
2. Start the Paper server.
3. Restart MySQL, then restart the Paper server.

**Checks**:
- [ ] With MySQL down: Console logs show “MySQL connection attempt failed. Retrying ... (X attempts left)” for 3 retries, then failure.
- [ ] With MySQL up: Logs show successful connection; `nexo_storage` table exists.
- [ ] No server crashes.

**Notes**:
- Copy log snippets for retry attempts and success/failure.
- Ensure MySQL credentials in `config.yml` are correct before testing.

### 7. Inventory Size Handling
**Steps**:
1. Set `rows: 5` in `config.yml`, place a Nexo block, and fill its inventory (45 slots).
2. Change `rows: 3`, run `/zunkernexo reload`, and right-click the block.
3. Change `rows: 6`, run `/zunkernexo reload`, and right-click again.

**Checks**:
- [ ] `rows: 3`: Menu shows 3 rows (27 slots), excess items drop, log shows: “Inventory at ... has 45 slots but config allows 27. Truncating.”
- [ ] `rows: 6`: Menu shows 6 rows (54 slots), no errors.
- [ ] Items persist within new size limits.
- [ ] No errors in console or in-game.

**Notes**:
- Count dropped items when truncating.
- Verify inventory contents after resizing.

### 8. Edge Cases
**Steps**:
1. Install Multiverse, place a Nexo block, then unload its world (`/mv unload <world>`).
2. Try interacting with or breaking the block’s location.
3. With two players, access the same block’s inventory simultaneously.
4. Stop MySQL during gameplay, then interact with the block.

**Checks**:
- [ ] Unloaded world: Log shows “Cannot load inventory ... World is null.”; no crashes.
- [ ] Concurrent access: Inventory saves correctly without data loss.
- [ ] MySQL downtime: Logs show SQL errors; plugin continues running.
- [ ] No unexpected behavior.

**Notes**:
- Record log messages for each case.
- Test with high player concurrency if possible.

### 9. Nexo Integration
**Steps**:
1. Run `/nexo list` to verify available Nexo block IDs.
2. Configure multiple Nexo block types in `config.yml` (if available), place, and test them.
3. Add an invalid `nexo-id` to `config.yml`, restart server.

**Checks**:
- [ ] Only configured `nexo-id` blocks trigger storage functionality.
- [ ] Invalid `nexo-id`: Log shows “Nexo-id ... is not a valid Nexo block ID. Skipping.”.
- [ ] Nexo’s default mechanics (e.g., drops) are unaffected.
- [ ] No errors in console or in-game.

**Notes**:
- List tested `nexo-id` values and their behavior.
- Verify Nexo commands work independently.

## Reporting Issues
For each test case, document:
- **Pass/Fail**: Did the feature work as expected?
- **Issues**: Describe errors, unexpected behavior, or discrepancies.
- **Steps to Reproduce**: Detail how to trigger any issues.
- **Logs**: Include relevant `latest.log` snippets (found in server’s `logs` folder).
- **Screenshots**: Capture in-game errors or incorrect behavior.
- **Environment**: Note any deviations (e.g., different Nexo version, no permissions plugin).

Example report format:
```
Test: Right-Click Interaction
Status: Failed
Issue: Menu opens with 4 rows instead of 5
Steps: Placed block with rows: 5, right-clicked
Log: [Paste log snippet]
Screenshot: [Attach image]
```

Submit reports to the development team with:
- Full `latest.log` file.
- Screenshots or videos (if applicable).
- Additional feedback (e.g., suggestions for left-click behavior).

## Additional Notes
- **Left-Click Behavior**: Currently displays storage info. Note if this is
