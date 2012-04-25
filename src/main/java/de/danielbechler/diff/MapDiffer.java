/*
 * Copyright 2012 Daniel Bechler
 *
 * This file is part of java-object-diff.
 *
 * java-object-diff is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-object-diff is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with java-object-diff.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.danielbechler.diff;

import de.danielbechler.diff.accessor.*;
import de.danielbechler.diff.node.*;
import de.danielbechler.util.Collections;

import java.util.*;

/**
 * Used to find differences between {@link Map Maps}
 *
 * @author Daniel Bechler
 */
final class MapDiffer extends AbstractDiffer
{
	public MapDiffer()
	{
		setDelegate(new DelegatingObjectDifferImpl(null, this, null));
	}

	public MapDiffer(final DelegatingObjectDiffer delegate)
	{
		super(delegate);
	}

	public MapNode compare(final Map<?, ?> modified, final Map<?, ?> base)
	{
		return compare(Node.ROOT, Instances.of(new RootAccessor(), modified, base));
	}

	public MapNode compare(final Node parentNode, final Instances instances)
	{
		final MapNode node = new MapNode(parentNode, instances.getSourceAccessor());

		if (getDelegate().isIgnored(parentNode, instances))
		{
			node.setState(Node.State.IGNORED);
			return node;
		}

		indexAll(instances, node);

		if (instances.getWorking() != null && instances.getBase() == null)
		{
			handleEntries(instances, node, instances.getWorking(Map.class).keySet());
			node.setState(Node.State.ADDED);
		}
		else if (instances.getWorking() == null && instances.getBase() != null)
		{
			handleEntries(instances, node, instances.getBase(Map.class).keySet());
			node.setState(Node.State.REMOVED);
		}
		else if (instances.areSame())
		{
			node.setState(Node.State.UNTOUCHED);
		}
		else
		{
			handleEntries(instances, node, findAddedKeys(instances));
			handleEntries(instances, node, findRemovedKeys(instances));
			handleEntries(instances, node, findKnownKeys(instances));
		}
		return node;
	}

	private static void indexAll(final Instances instances, final MapNode node)
	{
		node.indexKeys(instances.getWorking(Map.class), instances.getBase(Map.class), instances.getFresh(Map.class));
	}

	private void handleEntries(final Instances instances, final MapNode parent, final Iterable<?> keys)
	{
		for (final Object key : keys)
		{
			handleEntries(key, instances, parent);
		}
	}

	private void handleEntries(final Object key, final Instances instances, final MapNode parent)
	{
		final Node node = compareEntry(key, instances, parent);
		if (node.hasChanges())
		{
			parent.setState(Node.State.CHANGED);
			parent.addChild(node);
		}
		else if (getConfiguration().isReturnUnchangedNodes())
		{
			parent.addChild(node);
		}
	}

	private Node compareEntry(final Object key, Instances instances, final MapNode parent)
	{
		final Accessor accessor = parent.accessorForKey(key);
		instances = instances.access(accessor);
		return getDelegate().delegate(parent, instances);
	}

	private static Collection<?> findAddedKeys(final Instances instances)
	{
		final Set<?> source = instances.getWorking(Map.class).keySet();
		final Set<?> filter = instances.getBase(Map.class).keySet();
		return Collections.filteredCopyOf(source, filter);
	}

	private static Collection<?> findRemovedKeys(final Instances instances)
	{
		final Set<?> source = instances.getBase(Map.class).keySet();
		final Set<?> filter = instances.getWorking(Map.class).keySet();
		return Collections.filteredCopyOf(source, filter);
	}

	private static Iterable<?> findKnownKeys(final Instances instances)
	{
		final Set<?> keys = instances.getWorking(Map.class).keySet();
		final Collection<?> changed = Collections.setOf(keys);
		changed.removeAll(findAddedKeys(instances));
		changed.removeAll(findRemovedKeys(instances));
		return changed;
	}
}
