package com.derdirk.carfinder;

import android.location.Address;

public class Utilities
{

  public Utilities()
  {
    // TODO Auto-generated constructor stub
  }
  
  public static CharSequence AddressToText(Address address)
  {
    final StringBuilder addressText = new StringBuilder();
    for (int i = 0, max = address.getMaxAddressLineIndex(); i < max; ++i)
    {
      addressText.append(address.getAddressLine(i));
      if ((i + 1) < max)
      {
        addressText.append(", ");
      }
    }
    //addressText.append(", ");
    //addressText.append(address.getCountryName());
    return addressText;
  }


}
