import React, { Component } from 'react';
import { Tree } from 'antd';
import LocatorTestModal from './LocatorTestModal';
import InspectorStyles from './Inspector.css';
import { withTranslation } from '../../util';

const IMPORTANT_ATTRS = [
  'name',
  'content-desc',
  'resource-id',
  'AXDescription',
  'AXIdentifier',
];

/**
 * Shows the 'source' of the app as a Tree
 */
class Source extends Component {

  getFormattedTag (el) {
    const {tagName, attributes} = el;
    let attrs = [];

    // Don't do translations on Source XML
    for (let attr of IMPORTANT_ATTRS) {
      if (attributes[attr]) {
        attrs.push(<span key={attr}>&nbsp;
          <i
            className={InspectorStyles.sourceAttrName}
          >{attr}</i>=<span
            className={InspectorStyles.sourceAttrValue}
          >&quot;{attributes[attr]}&quot;</span>
        </span>);
      }
    }
    return <span>
      &lt;<b className={InspectorStyles.sourceTag}>{tagName}</b>{attrs}&gt;
    </span>;
  }

  /**
   * Binds to antd Tree onSelect. If an item is being unselected, path is undefined
   * otherwise 'path' refers to the element's path.
   */
  handleSelectElement (path) {
    const {selectElement, unselectElement} = this.props;

    if (!path) {
      unselectElement();
    } else {
      selectElement(path);
    }
  }

  render () {
    const {
      source,
      sourceError,
      setExpandedPaths,
      expandedPaths,
      selectedElement = {},
      t,
    } = this.props;
    const {path} = selectedElement;

    // Recursives through the source and renders a TreeNode for an element
    let recursive = (elemObj) => {
      if (!((elemObj || {}).children || []).length) {return null;}

      return elemObj.children.map((el) => ({
        title: this.getFormattedTag(el),
        key: el.path,
        children: recursive(el),
      }));
    };

    const treeData = source && recursive(source);

    return <div id='sourceContainer' className={InspectorStyles['tree-container']} tabIndex="0">
      {/* Must switch to a new antd Tree component when there's changes to treeData  */}
      {treeData ?
        <Tree
          defaultExpandAll={true}
          onExpand={setExpandedPaths}
          expandedKeys={expandedPaths}
          onSelect={(selectedPaths) => this.handleSelectElement(selectedPaths[0])}
          selectedKeys={[path]}
          treeData={treeData} />
        :
        <Tree
          treeData={[]} />
      }
      {!source && !sourceError &&
        <i>{t('Gathering initial app source…')}</i>
      }
      {
        sourceError && t('couldNotObtainSource', {errorMsg: JSON.stringify(sourceError)})
      }
      <LocatorTestModal {...this.props} />
    </div>;
  }
}

export default withTranslation(Source);
