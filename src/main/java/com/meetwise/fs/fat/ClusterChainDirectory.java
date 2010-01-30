/*
 * $Id: ClusterChainDirectory.java 4975 2009-02-02 08:30:52Z lsantha $
 *
 * Copyright (C) 2003-2009 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package com.meetwise.fs.fat;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A directory that is stored in a cluster chain.
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
class ClusterChainDirectory extends AbstractDirectory {
    private final ClusterChain chain;
    
    protected ClusterChainDirectory(ClusterChain chain, boolean isRoot) {
        
        super((int)(chain.getLengthOnDisk() / AbstractDirectoryEntry.SIZE),
                chain.isReadOnly(), isRoot);
        
        this.chain = chain;   
    }
    
    public static ClusterChainDirectory read(
            ClusterChain chain, boolean root) throws IOException {
        
        final ClusterChainDirectory result =
                new ClusterChainDirectory(chain, root);
        
        result.read();
        return result;
    }
    
    public static ClusterChainDirectory create(ClusterChain cc,
            long parentCluster, boolean root) throws IOException {

        cc.setChainLength(1);
        final ClusterChainDirectory result =
                new ClusterChainDirectory(cc, root);
                
        if (!root)
            result.addDotEntries(cc.getStartCluster(), parentCluster);
        
        result.flush();
        return result;
    }
    
    /**
     * Sets the first two entries '.' and '..' in the directory
     *
     * @param myCluster
     * @param parentCluster
     */
    protected final void addDotEntries(long myCluster, long parentCluster)
            throws IOException {
        
        final AbstractDirectoryEntry dot = new AbstractDirectoryEntry(this);
        dot.setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        final FatDirEntry dotEntry = new FatDirEntry(dot);
        dotEntry.setName(ShortName.DOT);
        dotEntry.setStartCluster((int) myCluster);
        addEntry(dot);

        final AbstractDirectoryEntry dotDot =
                new AbstractDirectoryEntry(this);
        dotDot.setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        final FatDirEntry dotDotEntry = new FatDirEntry(dotDot);
        dotDotEntry.setName(ShortName.DOT_DOT);
        dotDotEntry.setStartCluster((int) parentCluster);
        addEntry(dotDot);
    }

    @Override
    protected final void read(ByteBuffer data) throws IOException {
        this.chain.readData(0, data);
    }

    @Override
    protected final void write(ByteBuffer data) throws IOException {
        final int toWrite = data.remaining();
        chain.writeData(0, data);
        final long trueSize = chain.getLengthOnDisk();
        
        if (trueSize > toWrite) {
            final int rest = (int) (trueSize - data.capacity());
            final ByteBuffer fill = ByteBuffer.allocate(rest);
            chain.writeData(data.capacity(), fill);
        }
    }

    /**
     * 
     * @return
     */
    @Override
    protected final long getStorageCluster() {
        return isRoot() ? 0 : chain.getStartCluster();
    }

    public final void delete() throws IOException {
        chain.setChainLength(0);
    }
    
    @Override
    protected final void changeSize(int entryCount)
            throws IOException, IllegalArgumentException {
            
        checkEntryCount(entryCount);
        
        final int size = entryCount * AbstractDirectoryEntry.SIZE;
        sizeChanged(chain.setSize(Math.max(size, chain.getClusterSize())));
    }
    
}
