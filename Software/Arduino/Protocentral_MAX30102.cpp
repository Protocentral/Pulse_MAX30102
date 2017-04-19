

#include "Arduino.h"
#include <Wire.h>
#include "Protocentral_MAX30102.h"

MAX30100::MAX30100(){

}

void MAX30100::setLEDs(pulseWidth pw, ledCurrent red, ledCurrent ir){
  uint8_t reg = I2CreadByte(MAX30100_ADDRESS, MAX30100_SPO2_CONFIG);
  reg = reg & 0xFC; // Set LED_PW to 00
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_SPO2_CONFIG, reg | pw);     // Mask LED_PW
 // I2CwriteByte(MAX30100_ADDRESS, MAX30100_LED_CONFIG, (red<<4) | ir); // write LED configs
}

void MAX30100::setSPO2(sampleRate sr){
  uint8_t reg = I2CreadByte(MAX30100_ADDRESS, MAX30100_SPO2_CONFIG);
  reg = reg & 0xE3; // Set SPO2_SR to 000
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_SPO2_CONFIG, reg | (sr<<2)); // Mask SPO2_SR
  reg = I2CreadByte(MAX30100_ADDRESS, MAX30100_MODE_CONFIG);
  reg = reg & 0xf8; // Set Mode to 000
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_SPO2_CONFIG, reg | 0x03); // Mask MODE
}

int MAX30100::getNumSamp(void){
    uint8_t wrPtr = I2CreadByte(MAX30100_ADDRESS, MAX30100_FIFO_WR_PTR);
    uint8_t rdPtr = I2CreadByte(MAX30100_ADDRESS, MAX30100_FIFO_RD_PTR);
    return (abs( 16 + wrPtr - rdPtr ) % 16);
}

void MAX30100::readSensor(void){
  uint8_t temp[6] = {0};  // Temporary buffer for read values
  I2CreadBytes(MAX30100_ADDRESS, MAX30100_FIFO_DATA, &temp[0], 6);  // Read four times from the FIFO
  //temp[0] = 0x11; temp[1] = 0x22; temp[2] = 0x33; temp[4] = 0x44; temp[5] = 0x55;temp[6] = 0x66;
  IR =  (long)((long)((long)temp[0]&0x03)<<16) | (long)temp[1]<<8 | (long)temp[2];    // Combine values to get the actual number
  RED = (long)((long)((long)temp[3] & 0x03)<<16) |(long)temp[4]<<8 | (long)temp[5];   // Combine values to get the actual number
}

void MAX30100::shutdown(void){
  uint8_t reg = I2CreadByte(MAX30100_ADDRESS, MAX30100_MODE_CONFIG);  // Get the current register
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_MODE_CONFIG, reg | 0x80);   
}

void MAX30100::reset(void){
  uint8_t reg = I2CreadByte(MAX30100_ADDRESS, MAX30100_MODE_CONFIG);  // Get the current register
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_MODE_CONFIG, reg | 0x40);   // mask the RESET bit
}

void MAX30100::startup(void){
  uint8_t reg = I2CreadByte(MAX30100_ADDRESS, MAX30100_MODE_CONFIG);  // Get the current register
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_MODE_CONFIG, reg & 0x7F);   // mask the SHDN bit
}

int MAX30100::getRevID(void){
  return I2CreadByte(MAX30100_ADDRESS, MAX30100_REV_ID);
}

int MAX30100::getPartID(void){
  return I2CreadByte(MAX30100_ADDRESS, MAX30100_PART_ID);
}

void MAX30100::begin(pulseWidth pw, ledCurrent ir, sampleRate sr){

  I2CwriteByte(MAX30100_ADDRESS, MAX30100_INT_ENABLE1, 0xc0); 
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_INT_ENABLE2, 0x00); 
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_FIFO_WR_PTR, 0x00); 
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_OVRFLOW_CTR, 0x00); 
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_FIFO_RD_PTR, 0x00); 
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_FIFO_CONFIG, 0x00); 
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_MODE_CONFIG, 0x03); 
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_SPO2_CONFIG, 0x07); 
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_LED1_AMP , 0x24); 
  I2CwriteByte(MAX30100_ADDRESS, MAX30100_LED2_AMP, 0x24); 
//  I2CwriteByte(MAX30100_ADDRESS, MAX30100_PILOT, 0x7f); 
  
}

void MAX30100::printRegisters(void){
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_INT_STATUS1),  BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_INT_STATUS2),  BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_FIFO_WR_PTR), BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_INT_ENABLE1), BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_OVRFLOW_CTR), BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_FIFO_RD_PTR),   BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_FIFO_DATA), BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_FIFO_CONFIG), BIN);
 // Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_LED_CONFIG),  BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_MODE_CONFIG),   BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_SPO2_CONFIG),   BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_LED1_AMP),      BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_LED2_AMP),     BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_PILOT ),     BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX310100_MLED_CTRL1),     BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX310100_MLED_CTRL2),     BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_TEMP_INTG),     BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS,  MAX30100_TEMP_FRAC),     BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS,MAX30100_DIE_TEMP),     BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_PROX_INT_TRESH),     BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_REV_ID),     BIN);
  Serial.println(I2CreadByte(MAX30100_ADDRESS, MAX30100_PART_ID),     BIN);
}

// Wire.h read and write protocols
void MAX30100::I2CwriteByte(uint8_t address, uint8_t subAddress, uint8_t data)
{
	Wire.beginTransmission(address);  // Initialize the Tx buffer
	Wire.write(subAddress);           // Put slave register address in Tx buffer
	Wire.write(data);                 // Put data in Tx buffer
	Wire.endTransmission();           // Send the Tx buffer
}

uint8_t MAX30100::I2CreadByte(uint8_t address, uint8_t subAddress)
{
	uint8_t data; // `data` will store the register data
	Wire.beginTransmission(address);         
	Wire.write(subAddress);	                 
	Wire.endTransmission(false);             
	Wire.requestFrom(address, (uint8_t) 1);  
	data = Wire.read();                      
	return data;                             
}

void MAX30100::I2CreadBytes(uint8_t address, uint8_t subAddress, uint8_t * dest, uint8_t count)
{
	Wire.beginTransmission(address);   // Initialize the Tx buffer
	// Next send the register to be read. OR with 0x80 to indicate multi-read.
	Wire.write(subAddress);     
	Wire.endTransmission(false);
	uint8_t i = 0;
	Wire.requestFrom(address, count);  // Read bytes from slave register address
	while (Wire.available())
	{
		dest[i++] = Wire.read(); 
	}
}
