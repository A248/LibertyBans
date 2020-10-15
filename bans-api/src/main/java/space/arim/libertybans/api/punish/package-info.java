/**
 * Defines API for drafting punishments as well as punishments themselves. <br>
 * <br>
 * <b>Active versus Historical Punishments</b>
 * An active punishment is neither expired nor undone.After being undone, a punishment is no longer active.
 * After a temporary punishment expires, it is no longer active. <br>
 * <br>
 * 'Historial' punishments include both those active, those expired, and those undone. All punishments,
 * regardless of whether they are active, are historical punishments.
 */
package space.arim.libertybans.api.punish;