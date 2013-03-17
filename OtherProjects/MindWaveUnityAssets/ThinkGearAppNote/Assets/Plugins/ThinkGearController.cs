using UnityEngine;
using System.Collections;

/**
 * The ThinkGearController class provides an event-based mechanism for scripts
 * in your Unity project to control and receive data from the headset. The available
 * events are as follows:
 *
 * Handled Events:
 * OnHeadsetConnectionRequest(string portName) - Initiate a headset connection
 * OnHeadsetDisconnectionRequest() - Initiate a headset disconnection
 *
 * Broadcasted Events:
 * OnHeadsetConnected()       - Sent when the headset has successfully connected
 * OnHeadsetDisconnected()    - Sent when the headset has been disconnected
 * OnHeadsetDataReceived(Hashtable data) - Sent when data is received from the headset
 * OnHeadsetConnectionError() - Sent when a connection attempt (triggered by 
 *                              OnHeadsetConnectionRequest) was unsuccessful
 *
 * Simply set your MonoBehavior up to broadcast and listen for these events, and
 * your Unity game is brainwave-enabled!
 *
 * Keep in mind that there is a fair amount of overhead to SendMessage() calls.
 * ThinkGearController uses SendMessage() to send events to all GOs in the scene
 * hierarchy, so this may impose a fairly hefty performance hit if you have a large
 * project. 
 *
 * If performance is a huge concern, a polling-based mechanism (where the TGController
 * maintains state, and another MonoBehaviour continuously polls the TGController
 * instance for state changes) should prove to be far more efficient and scalable.
 *
 * Alternatively, you can modify the TriggerEvent() method to restrict the scope of
 * its message sending, by invoking SendMessage() or BroadcastMessage() *only* on the
 * local GO instance (i.e. gameObject.SendMessage("SomeEvent")).
 */
public class ThinkGearController: MonoBehaviour {

  private int handleID = -1;
  private int baudRate = ThinkGear.BAUD_9600;
  private int packetType = ThinkGear.STREAM_PACKETS;

  // When we receive a headset connection request (e.g. from a GUI control)
  // then attempt to connect to the headset
  //
  // The IEnumerator is used in the 'yield' statement, to busy-wait the thread
  IEnumerator OnHeadsetConnectionRequest(string portName){
    handleID = ThinkGear.TG_GetNewConnectionId();

    int connectStatus = ThinkGear.TG_Connect(handleID,
                                             portName,
                                             baudRate,
                                             packetType);

    // check that the connection attempt returned successfully. if not,
    // this probably means that either the serial port does not exist,
    // or that the headset is not turned on
    if(connectStatus >= 0){
      // now we need to check that the headset is returning valid data.
      // the headset transmits data every second, so sleep for some 
      // interval longer than that to guarantee data received in the 
      // serial buffer
      //
      // we use the Unity-specific yield statement here so that the
      // thread doesn't block everything else while it's sleeping, 
      // like Thread.Sleep() would have.
      yield return new WaitForSeconds(1.5f);

      int packetCount = ThinkGear.TG_ReadPackets(handleID, -1);

      // if we received some packets, then the connection attempt was successful
      // notify the GOs in the game
      if(packetCount > 0){
        TriggerEvent("OnHeadsetConnected", null);

        // now set up a repeating invocation to update the headset data
        InvokeRepeating("UpdateHeadsetData", 0.0f, 1.0f);
      }
      // if we didn't find anything, then the connection attempt
      // failed. notify the rest of the GOs in the game
      else {
        TriggerEvent("OnHeadsetConnectionError", null);

        // free the handle
        ThinkGear.TG_FreeConnection(handleID);
      }
    }
    // trigger an error event if the connection attempt did not return
    // successfully
    else {
      TriggerEvent("OnHeadsetConnectionError", null);

      ThinkGear.TG_FreeConnection(handleID);
    }
  }
   
  // when we receive a headset disconnection request, attempt to 
  // disconnect.
  void OnHeadsetDisconnectionRequest(){
    ThinkGear.TG_FreeConnection(handleID);
    CancelInvoke("UpdateHeadsetData");

    TriggerEvent("OnHeadsetDisconnected", null);
  }

  // reconfigure the parameters that the headset is initialized with.
  void OnHeadsetReconfigurationRequest(Hashtable parameters){
    //baudRate = parameters["baudRate"];
  }

  // Repeating callback method to retrieve data from the headset
  private void UpdateHeadsetData(){
    int packetCount = ThinkGear.TG_ReadPackets(handleID, -1);

    Hashtable values = new Hashtable();

    if(packetCount > 0){
      values.Add("poorSignal", GetDataValue(ThinkGear.DATA_POOR_SIGNAL));  
      values.Add("attention", GetDataValue(ThinkGear.DATA_ATTENTION));
      values.Add("meditation", GetDataValue(ThinkGear.DATA_MEDITATION));
      values.Add("delta", GetDataValue(ThinkGear.DATA_DELTA));
      values.Add("theta", GetDataValue(ThinkGear.DATA_THETA));
      values.Add("lowAlpha", GetDataValue(ThinkGear.DATA_ALPHA1));
      values.Add("highAlpha", GetDataValue(ThinkGear.DATA_ALPHA2));
      values.Add("lowBeta", GetDataValue(ThinkGear.DATA_BETA1));
      values.Add("highBeta", GetDataValue(ThinkGear.DATA_BETA2));
      values.Add("lowGamma", GetDataValue(ThinkGear.DATA_GAMMA1));
      values.Add("highGamma", GetDataValue(ThinkGear.DATA_GAMMA2));

      TriggerEvent("OnHeadsetDataReceived", values);
    }

  }

  // Convenience method to trigger an event on all GOs
  private void TriggerEvent(string eventName, System.Object parameter){
    foreach(GameObject go in FindObjectsOfType(typeof(GameObject)))
      go.SendMessage(eventName, parameter, 
                     SendMessageOptions.DontRequireReceiver); 
  }

  // Convenience method to retrieve a value from the headset
  private float GetDataValue(int valueType){
    return ThinkGear.TG_GetValue(handleID, valueType);
  }
}
