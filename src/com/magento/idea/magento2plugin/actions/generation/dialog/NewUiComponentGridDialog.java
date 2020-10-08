/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.generation.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.magento.idea.magento2plugin.actions.generation.NewUiComponentFormAction;
import com.magento.idea.magento2plugin.actions.generation.NewUiComponentGridAction;
import com.magento.idea.magento2plugin.actions.generation.data.ControllerFileData;
import com.magento.idea.magento2plugin.actions.generation.data.LayoutXmlData;
import com.magento.idea.magento2plugin.actions.generation.data.MenuXmlData;
import com.magento.idea.magento2plugin.actions.generation.data.UiComponentDataProviderData;
import com.magento.idea.magento2plugin.actions.generation.data.UiComponentGridData;
import com.magento.idea.magento2plugin.actions.generation.data.UiComponentGridToolbarData;
import com.magento.idea.magento2plugin.actions.generation.dialog.validator.NewUiComponentGridDialogValidator;
import com.magento.idea.magento2plugin.actions.generation.generator.LayoutXmlGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.MenuXmlGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.ModuleControllerClassGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.UiComponentDataProviderGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.UiComponentGridXmlGenerator;
import com.magento.idea.magento2plugin.actions.generation.generator.util.NamespaceBuilder;
import com.magento.idea.magento2plugin.indexes.ModuleIndex;
import com.magento.idea.magento2plugin.magento.files.ControllerBackendPhp;
import com.magento.idea.magento2plugin.magento.files.UiComponentDataProviderPhp;
import com.magento.idea.magento2plugin.magento.packages.Areas;
import com.magento.idea.magento2plugin.magento.packages.File;
import com.magento.idea.magento2plugin.magento.packages.HttpMethod;
import com.magento.idea.magento2plugin.magento.packages.Package;
import com.magento.idea.magento2plugin.stubs.indexes.xml.MenuIndex;
import com.magento.idea.magento2plugin.ui.FilteredComboBox;
import com.magento.idea.magento2plugin.util.magento.GetModuleNameByDirectoryUtil;
import com.magento.idea.magento2plugin.util.magento.GetResourceCollections;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

@SuppressWarnings({"PMD.TooManyFields", "PMD.ExcessiveImports", "PMD.UnusedPrivateMethod"})
public class NewUiComponentGridDialog extends AbstractDialog {
    private final Project project;
    private final String moduleName;
    private final NewUiComponentGridDialogValidator validator;
    private List<String> collectionOptions;
    private JPanel contentPanel;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField uiComponentName;
    private JTextField idField;
    private JCheckBox addToolBar;
    private JCheckBox addBookmarksCheckBox;
    private JCheckBox addColumnsControlCheckBox;
    private JCheckBox addFullTextSearchCheckBox;
    private JCheckBox addListingFiltersCheckBox;
    private JCheckBox addListingPagingCheckBox;
    private FilteredComboBox collection;
    private FilteredComboBox dataProviderType;
    private FilteredComboBox areaSelect;
    private JTextField providerClassName;
    private JTextField dataProviderParentDirectory;
    private JTextField acl;
    private JLabel aclLabel;
    private JLabel routeLabel;
    private JLabel controllerLabel;
    private JLabel actionLabel;
    private JTextField route;
    private JTextField controllerName;
    private JTextField actionName;
    private JLabel parentMenuItemLabel;
    private JTextField sortOrder;
    private JTextField menuIdentifier;
    private JLabel sortOrderLabel;
    private JLabel menuIdentifierLabel;
    private JTextField menuTitle;
    private JLabel menuTitleLabel;
    private FilteredComboBox parentMenu;
    private JLabel collectionLabel;

    /**
     * New UI component grid dialog constructor.
     *
     * @param project Project
     * @param directory PsiDirectory
     */
    public NewUiComponentGridDialog(final Project project, final PsiDirectory directory) {
        super();
        this.project = project;
        this.validator = NewUiComponentGridDialogValidator.getInstance();
        this.moduleName = GetModuleNameByDirectoryUtil.execute(directory, project);

        setContentPane(contentPanel);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        addActionListeners();
        setDefaultValues();

        buttonOK.addActionListener(event -> onOK());
        buttonCancel.addActionListener(event -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent event) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPanel.registerKeyboardAction(
                event -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
        menuIdentifier.setText(getModuleName() + "::listing");
    }

    /**
     * Open new UI component grid dialog.
     *
     * @param project Project
     * @param directory PsiDirectory
     */
    public static void open(final Project project, final PsiDirectory directory) {
        final NewUiComponentGridDialog dialog = new NewUiComponentGridDialog(project, directory);
        dialog.pack();
        dialog.centerDialog(dialog);
        dialog.setVisible(true);
    }

    /**
     * Get grid data provider data.
     *
     * @return UiComponentGridDataProviderData
     */
    public UiComponentDataProviderData getGridDataProviderData() {
        return new UiComponentDataProviderData(
                getDataProviderType(),
                getDataProviderClass(),
                getDataProviderNamespace(),
                getDataProviderDirectory(),
                getCollection()
        );
    }

    /**
     * Get grid toolbar data.
     *
     * @return UiComponentGridToolbarData
     */
    public UiComponentGridToolbarData getUiComponentGridToolbarData() {
        return new UiComponentGridToolbarData(
                getAddToolBar(),
                getAddBookmarksCheckBox(),
                getAddColumnsControlCheckBox(),
                getAddFullTextSearchCheckBox(),
                getAddListingFiltersCheckBox(),
                getAddListingPagingCheckBox()
        );
    }

    /**
     * Get grid UI component data.
     *
     * @return UiComponentGridData
     */
    public UiComponentGridData getUiComponentGridData() {
        return new UiComponentGridData(
                getModuleName(),
                getArea(),
                getUiComponentName(),
                getDataProviderClassFqn(),
                getEntityIdFieldName(),
                getAcl(),
                getUiComponentGridToolbarData()
        );
    }

    protected void onCancel() {
        dispose();
    }

    private void onOK() {
        if (!validator.validate(this)) {
            return;
        }

        generateViewControllerFile();
        generateLayoutFile();
        generateMenuFile();
        generateUiComponentFile();
        this.setVisible(false);
    }

    private void setDefaultValues() {
        dataProviderParentDirectory.setText("Ui/Component/Listing");
    }

    private void addActionListeners() {
        addToolBar.addActionListener(event -> onAddToolBarChange());
        areaSelect.addActionListener(event -> onAreaChange());
        dataProviderType.addActionListener(event -> onDataProviderTypeChange());
    }

    private void generateUiComponentFile() {
        final UiComponentDataProviderGenerator dataProviderGenerator;
        dataProviderGenerator = new UiComponentDataProviderGenerator(
                getGridDataProviderData(),
                getModuleName(),
                project
        );
        final UiComponentGridXmlGenerator gridXmlGenerator = new UiComponentGridXmlGenerator(
                getUiComponentGridData(),
                project
        );
        dataProviderGenerator.generate(NewUiComponentGridAction.ACTION_NAME);
        gridXmlGenerator.generate(NewUiComponentGridAction.ACTION_NAME, true);
    }

    private PsiFile generateViewControllerFile() {
        final NamespaceBuilder namespace = new NamespaceBuilder(
                getModuleName(),
                getActionName(),
                getControllerDirectory()
        );
        return new ModuleControllerClassGenerator(new ControllerFileData(
                getControllerDirectory(),
                getActionName(),
                getModuleName(),
                getArea(),
                HttpMethod.GET.toString(),
                getAcl(),
                true,
                namespace.getNamespace()
        ), project).generate(NewUiComponentFormAction.ACTION_NAME, false);
    }

    private PsiFile generateLayoutFile() {
        return new LayoutXmlGenerator(new LayoutXmlData(
            getArea(),
            getRoute(),
            getModuleName(),
            getControllerName(),
            getActionName(),
            getUiComponentName()
        ), project).generate(NewUiComponentFormAction.ACTION_NAME, false);
    }

    private PsiFile generateMenuFile() {
        return new MenuXmlGenerator(new MenuXmlData(
            getParentMenuItem(),
            getSortOrder(),
            getModuleName(),
            getMenuIdentifier(),
            getMenuTitle(),
            getAcl(),
            getMenuAction()
        ), project).generate(NewUiComponentFormAction.ACTION_NAME, false);
    }

    private String getModuleName() {
        return moduleName;
    }

    private void onAddToolBarChange() {
        final boolean enabled = getAddToolBar();

        addBookmarksCheckBox.setEnabled(enabled);
        addColumnsControlCheckBox.setEnabled(enabled);
        addFullTextSearchCheckBox.setEnabled(enabled);
        addListingFiltersCheckBox.setEnabled(enabled);
        addListingPagingCheckBox.setEnabled(enabled);
    }

    private void onAreaChange() {
        final boolean visible = getArea().equals(Areas.adminhtml.toString());
        acl.setVisible(visible);
        aclLabel.setVisible(visible);
    }

    private void onDataProviderTypeChange() {
        final boolean visible = getDataProviderType().equals(
                UiComponentDataProviderPhp.COLLECTION_TYPE
        );

        collection.setVisible(visible);
        collectionLabel.setVisible(visible);
    }

    @SuppressWarnings({"PMD.UnusedPrivateMethod"})
    private void createUIComponents() {
        this.collection = new FilteredComboBox(getCollectionOptions());
        this.dataProviderType = new FilteredComboBox(getProviderTypeOptions());
        this.areaSelect = new FilteredComboBox(getAreaOptions());
        this.parentMenu = new FilteredComboBox(getMenuReferences());
    }

    @NotNull
    private ArrayList<String> getMenuReferences() {
        final Collection<String> menuReferences
                = FileBasedIndex.getInstance().getAllKeys(MenuIndex.KEY, project);
        ArrayList<String> menuReferencesList = new ArrayList<>(menuReferences);
        Collections.sort(menuReferencesList);
        return menuReferencesList;
    }

    private List<String> getCollectionOptions() {
        if (this.collectionOptions == null) {
            this.collectionOptions = new ArrayList<>();
            this.collectionOptions.add("");
            final GetResourceCollections getResourceCollections;
            getResourceCollections = GetResourceCollections.getInstance(
                    this.project
            );

            for (final PhpClass collectionClass: getResourceCollections.execute()) {
                this.collectionOptions.add(collectionClass.getFQN());
            }
        }

        return this.collectionOptions;
    }

    private List<String> getProviderTypeOptions() {
        return new ArrayList<>(
                Arrays.asList(
                        UiComponentDataProviderPhp.CUSTOM_TYPE,
                        UiComponentDataProviderPhp.COLLECTION_TYPE
                )
        );
    }

    private List<String> getAreaOptions() {
        return new ArrayList<>(
                Arrays.asList(
                        Areas.adminhtml.toString(),
                        Areas.base.toString(),
                        Areas.frontend.toString()
                )
        );
    }

    private String getDataProviderNamespace() {
        final String[]parts = moduleName.split(Package.vendorModuleNameSeparator);
        if (parts[0] == null || parts[1] == null || parts.length > 2) {
            return null;
        }
        final String directoryPart = getDataProviderDirectory().replace(
                File.separator,
                Package.fqnSeparator
        );

        return String.format(
                "%s%s%s%s%s",
                parts[0],
                Package.fqnSeparator,
                parts[1],
                Package.fqnSeparator,
                directoryPart
        );
    }

    private String getDataProviderClassFqn() {
        return String.format(
                "%s%s%s",
                getDataProviderNamespace(),
                Package.fqnSeparator,
                getDataProviderClass()
        );
    }

    private Boolean getAddToolBar() {
        return addToolBar.isSelected();
    }

    private Boolean getAddColumnsControlCheckBox() {
        return addColumnsControlCheckBox.isSelected();
    }

    private Boolean getAddFullTextSearchCheckBox() {
        return addFullTextSearchCheckBox.isSelected();
    }

    private Boolean getAddListingFiltersCheckBox() {
        return addListingFiltersCheckBox.isSelected();
    }

    private Boolean getAddListingPagingCheckBox() {
        return addListingPagingCheckBox.isSelected();
    }

    private Boolean getAddBookmarksCheckBox() {
        return addBookmarksCheckBox.isSelected();
    }

    private String getDataProviderType() {
        return dataProviderType.getSelectedItem().toString();
    }

    private String getArea() {
        return areaSelect.getSelectedItem().toString();
    }

    private String getUiComponentName() {
        return uiComponentName.getText().toString();
    }

    private String getAcl() {
        return acl.getText().toString();
    }

    private String getEntityIdFieldName() {
        return idField.getText().toString();
    }

    private String getCollection() {
        final String collectionFqn = collection.getSelectedItem().toString().trim();

        if (collectionFqn.length() == 0) {
            return "";
        }

        return collectionFqn.substring(1);
    }

    private String getDataProviderClass() {
        return providerClassName.getText().trim();
    }

    private String getDataProviderDirectory() {
        return dataProviderParentDirectory.getText().trim();
    }

    public String getActionName() {
        return actionName.getText().trim();
    }

    private String getControllerDirectory() {
        final String directory = ControllerBackendPhp.DEFAULT_DIR;

        return directory + File.separator + getControllerName();
    }

    public String getControllerName() {
        return controllerName.getText().trim();
    }

    public String getRoute() {
        return route.getText().trim();
    }

    private String getParentMenuItem() {
        return parentMenu.getSelectedItem().toString();
    }

    public String getSortOrder() {
        return sortOrder.getText().trim();
    }

    public String getMenuIdentifier() {
        return menuIdentifier.getText().trim();
    }

    private String getMenuAction() {
        return getRoute()
            + File.separator
            + getControllerName().toLowerCase()
            + File.separator
            + getActionName().toLowerCase();
    }

    public String getMenuTitle() {
        return menuTitle.getText().trim();
    }
}
