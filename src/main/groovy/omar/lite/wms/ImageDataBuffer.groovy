package omar.lite.wms

import groovy.transform.CompileStatic
import joms.oms.ossimImageDataRefPtr
import joms.oms.ossimInterleaveType
import joms.oms.ossimScalarType
import java.awt.image.DataBuffer
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferDouble
import java.awt.image.DataBufferFloat
import java.awt.image.DataBufferInt
import java.awt.image.DataBufferShort
import java.awt.image.DataBufferUShort
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

@CompileStatic
class ImageDataBuffer {
  ossimImageDataRefPtr imageData
  DataBuffer dataBuffer
  ossimInterleaveType interleaveType

  ImageDataBuffer( ossimImageDataRefPtr imageData = null, ossimInterleaveType interleaveType = ossimInterleaveType.OSSIM_BSQ ) {
    this.imageData = imageData
    this.interleaveType = interleaveType

    if ( imageData != null ) {
      initialize()
    }
  }

  private void initialize() {
    if ( imageData?.get() == null ) {
      this.dataBuffer = null
      return
    }
    ossimScalarType scalarType = imageData?.scalarType
    int width = imageData?.width?.intValue()
    int height = imageData?.height?.intValue()
    int numBands = imageData?.numberOfBands?.intValue()
    int sizeInBytes = imageData?.sizeInBytes?.intValue()
    int numElems = width * height * numBands

    switch ( scalarType ) {
    case ossimScalarType.OSSIM_UINT8: /* 8 bit unsigned integer */
    case ossimScalarType.OSSIM_SINT8: /* 8 bit signed integer */
    case ossimScalarType.OSSIM_UCHAR: /* 8 bit unsigned integer */
      ByteBuffer buffer = unloadTile( sizeInBytes )
      byte[] data

      if ( buffer?.hasArray() ) {
        data = buffer?.array()
      } else {
        data = new byte[numElems]
        buffer?.get( data )
      }

      dataBuffer = new DataBufferByte( data, data.length )
      break
    case ossimScalarType.OSSIM_SINT16:   /* 16 bit signed integer */
    case ossimScalarType.OSSIM_CINT16:   /* 16 bit complex integer */
    case ossimScalarType.OSSIM_SSHORT16: /* 16 bit signed integer */
      ShortBuffer buffer = unloadTile( sizeInBytes )?.asShortBuffer()
      short[] data

      if ( buffer?.hasArray() ) {
        data = buffer?.array()
      } else {
        data = new short[numElems]
        buffer?.get( data )
      }

      dataBuffer = new DataBufferShort( data, data.length )
      break
    case ossimScalarType.OSSIM_UINT9:    /* 16 bit unsigned integer (9 bits used) */
    case ossimScalarType.OSSIM_UINT10:   /* 16 bit unsigned integer (10 bits used) */
    case ossimScalarType.OSSIM_UINT11:   /* 16 bit unsigned integer (11 bits used) */
    case ossimScalarType.OSSIM_UINT12:   /* 16 bit unsigned integer (12 bits used) */
    case ossimScalarType.OSSIM_UINT13:   /* 16 bit unsigned integer (13 bits used) */
    case ossimScalarType.OSSIM_UINT14:   /* 16 bit unsigned integer (14 bits used) */
    case ossimScalarType.OSSIM_UINT15:   /* 16 bit unsigned integer (15 bits used) */
    case ossimScalarType.OSSIM_UINT16:   /* 16 bit unsigned integer */
    case ossimScalarType.OSSIM_USHORT11: /* 11 bit unsigned integer */
      ShortBuffer buffer = unloadTile( sizeInBytes )?.asShortBuffer()
      short[] data

      if ( buffer?.hasArray() ) {
        data = buffer?.array()
      } else {
        data = new short[numElems]
        buffer?.get( data )
      }

      dataBuffer = new DataBufferUShort( data, data.length )
      break
    case ossimScalarType.OSSIM_FLOAT32:          /* 32 bit floating point */
    case ossimScalarType.OSSIM_CFLOAT32:         /* 32 bit complex floating point */
    case ossimScalarType.OSSIM_NORMALIZED_FLOAT: /* 32 bit normalized floating point */
    case ossimScalarType.OSSIM_FLOAT:            /* 32 bit floating point */
      FloatBuffer buffer = unloadTile( sizeInBytes )?.asFloatBuffer()
      float[] data

      if ( buffer?.hasArray() ) {
        data = buffer?.array()
      } else {
        data = new float[numElems]
        buffer?.get( data )
      }

      dataBuffer = new DataBufferFloat( data, data.length )
      break
    case ossimScalarType.OSSIM_UINT32: /* 32 bit unsigned integer */
    case ossimScalarType.OSSIM_SINT32: /* 32 bit signed integer */
    case ossimScalarType.OSSIM_CINT32: /* 32 bit complex integer */
      IntBuffer buffer = unloadTile( sizeInBytes )?.asIntBuffer()
      int[] data

      if ( buffer?.hasArray() ) {
        data = buffer?.array()
      } else {
        data = new float[numElems]
        buffer?.get( data )
      }

      dataBuffer = new DataBufferInt( data, data.length )
      break
    case ossimScalarType.OSSIM_FLOAT64:           /* 64 bit floating point */
    case ossimScalarType.OSSIM_CFLOAT64:          /* 64 bit complex floating point */
    case ossimScalarType.OSSIM_NORMALIZED_DOUBLE: /* 64 bit normalized floating point */
    case ossimScalarType.OSSIM_DOUBLE:            /* 64 bit floating point */
      DoubleBuffer buffer = unloadTile( sizeInBytes )?.asDoubleBuffer()
      double[] data

      if ( buffer?.hasArray() ) {
        data = buffer?.array()
      } else {
        data = new double[numElems]
        buffer?.get( data )
      }

      dataBuffer = new DataBufferDouble( data, data.length )
      break
    }
  }

  private ByteBuffer unloadTile( int sizeInBytes ) {
    ByteBuffer buffer = ByteBuffer.allocateDirect( sizeInBytes )
//    ByteBuffer buffer = ByteBuffer.allocate( sizeInBytes )

    imageData.unloadTile( buffer, imageData?.imageRectangle, interleaveType )

    return buffer
  }
}